package com.example.board.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
public class StoredNote {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private int x;
    private int y;
    private String filename;
    private float angle;

    @Lob
    private byte[] image;

    public StoredNote() {

    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
    
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public long getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public byte[] getImage() {
        return image;
    }

    public String getFilename() {
        return filename;
    }

    public float getAngle() {
        return angle;
    }
}
