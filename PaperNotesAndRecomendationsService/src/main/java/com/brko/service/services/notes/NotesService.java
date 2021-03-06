package com.brko.service.services.notes;

import com.brko.service.persistance.datamodel.Note;
import com.brko.service.persistance.datamodel.User;

import java.util.List;

/**
 * Created by ppetrov on 11/4/2016.
 */
public interface NotesService {

    /**
     * Save note for specific user.
     * @param text note value
     * @param user user
     */
    void saveNote(String text, User user);

    /**
     * Edit note for some user
     * @param note new note content
     */
    void editNote(Note note);

    /**
     * Return notes for user with specific email.
     * @param email the Email
     * @return List of notes
     */
    List<Note> getNotesByUserEmail(String email);
}
