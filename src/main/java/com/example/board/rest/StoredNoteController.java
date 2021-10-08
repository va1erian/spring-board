package com.example.board.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.example.board.jpa.StoredNote;
import com.example.board.jpa.StoredNoteRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class StoredNoteController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoredNoteController.class);

    public record Note(long id, int x, int y, float angle) {

        static Note fromStoredNote(StoredNote sn) {
            return new Note(sn.getId(), sn.getX(), sn.getY(), sn.getAngle());
        }
    }


    @Autowired
    StoredNoteRepository noteRepo;

    Set<SseEmitter> listeners = Collections.newSetFromMap(new ConcurrentHashMap<SseEmitter,Boolean>(10));


    @GetMapping("/notes-events")
    public SseEmitter noteEvents() {
        SseEmitter sseEmitter = new SseEmitter(180_000L);
        listeners.add(sseEmitter);
        LOGGER.info("new sse emitter");

        sseEmitter.onCompletion(() ->  {
            LOGGER.info("SseEmitter is completed");
            listeners.remove(sseEmitter);
        });
        sseEmitter.onTimeout(() -> LOGGER.info("SseEmitter is timed out"));

        sseEmitter.onError((ex) -> LOGGER.info("SseEmitter got error:", ex));
        return sseEmitter;
    }

    @GetMapping("/notes")
    public List<Note> getNotes() {
        List<Note> result = new ArrayList<>();
        noteRepo.findAll().forEach((sn) -> result.add(Note.fromStoredNote(sn)));

        return result;
    }

    @PatchMapping("/notes")
    @Transactional
    public ResponseEntity<Void> updateNote(@RequestBody Note updatedNote) {
        StoredNote sn = new StoredNote();

        noteRepo.setPositionForNote(updatedNote.x, updatedNote.y, updatedNote.angle, updatedNote.id );
        sn.setX(updatedNote.x);
        sn.setY(updatedNote.y);
        sn.setId(updatedNote.id);
        sn.setAngle(updatedNote.angle);
        notifyNewNote(sn);
        return ResponseEntity.ok().build();
    }
    
	@GetMapping("/notes/{id}")
	@ResponseBody
	public ResponseEntity<byte[]> serveFile(@PathVariable long id) {
        StoredNote sn = noteRepo.findById(id).orElseThrow();
        String fileExtension = getFileExtension(sn.getFilename());
		
        byte[] imageBytes = sn.getImage();
        switch(fileExtension) {
            case "jpg":
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
            case "gif":
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .body(imageBytes);
            case "png":
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
            default:
            return ResponseEntity.ok()
                .body(imageBytes);
        }
	}

    @PostMapping("/notes")
    public void postNotes(@RequestHeader("X-Mouse-X") int mouseX, @RequestHeader("X-Mouse-Y") int mouseY, @RequestParam("file") MultipartFile file) {
        LOGGER.info(file.getSize() + "bytes file");
        LOGGER.info(file.getOriginalFilename());
        LOGGER.info("mouseX " + mouseX);
        LOGGER.info("mouseY " + mouseY);

        StoredNote sn = new StoredNote();
        try {
            sn.setImage(file.getBytes());
            sn.setX(mouseX);
            sn.setY(mouseY);
            sn.setFilename(file.getOriginalFilename());
            noteRepo.save(sn);
            notifyNewNote(sn);
        } catch (IOException e) {
            LOGGER.error("An error occurred while saving notes!", e);
        }

    }

    private String getFileExtension(String filename) {
        int suffixPos = filename.lastIndexOf('.');
        return filename.substring(suffixPos + 1).toLowerCase();
    }

    private void notifyNewNote(StoredNote sn) {
        listeners.forEach((sse) -> {
            try {
                LOGGER.info("notify!");
                sse.send(
                    SseEmitter.event()
                        .id(Long.toString(sn.getId()))
                        .data(Note.fromStoredNote(sn), MediaType.APPLICATION_JSON)
                    );
            } catch (IOException e) {
                LOGGER.error("An error occurred while emitting progress.", e);
            }
        });
    }
}
