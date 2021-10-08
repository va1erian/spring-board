let notes = new Map();
var mouseX, mouseY;


function dropHandler(ev) {
  console.log('File(s) dropped');
  let dropZone = document.querySelector("#drop_zone");
  var rect = dropZone.getBoundingClientRect();
  mouseX = ev.pageX - rect.left;
  mouseY = ev.pageY - rect.top;

  // Prevent default behavior (Prevent file from being opened)
  ev.preventDefault();

  if (ev.dataTransfer.items) {
    console.log("item list");
    // Use DataTransferItemList interface to access the file(s)
    console.log(ev.dataTransfer);

    for (var i = 0; i < ev.dataTransfer.items.length; i++) {
        //dragged a note
        if (ev.dataTransfer.items[i].kind === 'string' && (ev.dataTransfer.items[i].type.match('^text/plain'))) {
        ev.dataTransfer.items[i].getAsString((s) => {
          let noteId = parseInt(s);
          console.log("got existing note ",noteId);
          notes.get(noteId).x = mouseX;
          notes.get(noteId).y = mouseY;
          sendUpdatedNote(parseInt(s));
          return; 
        });
      } else if (ev.dataTransfer.items[i].kind === 'file') {
        var file = ev.dataTransfer.items[i].getAsFile();
        console.log('... file[' + i + '].name = ' + file.name);
        uploadFile(file);
      }
    }
  } else {
    // Use DataTransfer interface to access the file(s)
    console.log(ev.dataTransfer);
    for (var i = 0; i < ev.dataTransfer.files.length; i++) {
      console.log('... file[' + i + '].name = ' + ev.dataTransfer.files[i].name);
    }
  }
}

function dragImageHandler(e, noteId) {
    e.dataTransfer
      .setData('text/plain', noteId);

}


function sendUpdatedNote(noteId) {
  let data = notes.get(noteId);

  console.log("sending update for ", data);
  fetch('/notes', {
    method: "PATCH", 
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data)
  });
}

function uploadFile(file) {
    console.log("uploading...");
    let formData = new FormData();
    formData.append("file", file);
    fetch('/notes', {method: "POST", body: formData, headers : {  'X-Mouse-X': mouseX, 'X-Mouse-Y': mouseY }});
}
  

function dragOverHandler(ev) {
    console.log('File(s) in drop zone');
  
    // Prevent default behavior (Prevent file from being opened)
    ev.preventDefault();
}

function noteIdFromId(id) {
  let underScore = id.lastIndexOf("_");
  return id.substring(underScore + 1);
}
function putNote(note) {
  notes.set(parseInt(note.id), note);
  let dropZone = document.querySelector("#drop_zone");
  dropZone.insertAdjacentHTML('beforeend', noteImageTmpl(note.id, note.x, note.y));

  var degree = (note.angle * (360 / (2 * Math.PI)));
  $(`#note_${note.id.toString()}`).css('transform', 'rotate(' + degree + 'deg)').css('transform-origin', '50% 50%');

  let el = $(`#note_${note.id.toString()} .handle`);
  var dragging = false,
  target_wp = $(`#note_${note.id.toString()}`),
  o_x, o_y, h_x, h_y, last_angle;

  el.mousedown((e) => {
    h_x = e.pageX;
    h_y = e.pageY; // clicked point
    e.preventDefault();
    e.stopPropagation();
    dragging = true;
    if (!target_wp.data("origin")) target_wp.data("origin", {
        left:  target_wp.offset().left,
        top:   target_wp.offset().top
    });
    o_x = target_wp.data("origin").left;
    o_y = target_wp.data("origin").top; // origin point
    
    last_angle = target_wp.data("last_angle") || 0;
    console.log(last_angle);
  } );

  document.addEventListener("mousemove",(e) => {
    if (dragging) {
      var s_x = e.pageX,
          s_y = e.pageY; // start rotate point
      if (s_x !== o_x && s_y !== o_y) { //start rotate
          var s_rad = Math.atan2(s_y - o_y, s_x - o_x); // current to origin
          s_rad -= Math.atan2(h_y - o_y, h_x - o_x); // handle to origin
          s_rad += last_angle; // relative to the last one
          var degree = (s_rad * (360 / (2 * Math.PI)));
          target_wp.css('transform', 'rotate(' + degree + 'deg)');
          target_wp.css('transform-origin', '50% 50%');
      }
    }
  });

  document.addEventListener("mouseup", (e) => {
      dragging = false
      var s_x = e.pageX,
          s_y = e.pageY;
      
      // Saves the last angle for future iterations
      var s_rad = Math.atan2(s_y - o_y, s_x - o_x); // current to origin
      s_rad -= Math.atan2(h_y - o_y, h_x - o_x); // handle to origin
      s_rad += last_angle;
      target_wp.data("last_angle", s_rad);
      console.log(target_wp, s_rad);
      notes.get(parseInt(note.id)).angle = s_rad;
      sendUpdatedNote(note.id);
      
  });

}

function noteImageTmpl(noteId, noteX, noteY) {
  return `<div id="note_${noteId.toString()}" draggable="true" class="note_block" style="top:${noteY.toString()}px; left:${noteX.toString()}px;" ondragstart="dragImageHandler(event, ${noteId.toString()});">` +
    `<img src="/notes/${noteId.toString()}" />` +
    " <div class='handle'> </div> " + 
    "</div>"
}

document.addEventListener("DOMContentLoaded", () => {
  fetch("/notes")
  .then(response => response.json())
  .then(response => { 
    console.log(response) 
    for(const i of response) {
      putNote(i);
    }
  });


  let dropZone = document.querySelector("#drop_zone");
  dropZone.addEventListener("drop", dropHandler, false);
  dropZone.addEventListener("dragover", dragOverHandler, false);

  let eventSource = new EventSource("/notes-events");

  eventSource.onopen = (event) => {
    console.log("connection opened")
  }
  
  eventSource.onmessage = (event) => {
    console.log("result", event.data);
    let msg = JSON.parse(event.data);
    let noteId = msg.id;
    let note = notes.get(noteId);
    if(note === undefined) {
      putNote(msg);
    } else {
      let degree = (msg.angle * (360 / (2 * Math.PI)));

      $(`#note_${noteId}`).animate({
        top: `${msg.y.toString()}px`,
        left:`${msg.x.toString()}px`,
      })
        .css('transform', 'rotate(' + degree + 'deg)')
        .css('transform-origin', '50% 50%');
    }
  }
  
  eventSource.onerror = (event) => {
    console.log(event.target.readyState)
    eventSource.close();
  }

});