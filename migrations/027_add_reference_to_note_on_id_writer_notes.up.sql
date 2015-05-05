ALTER TABLE objective8.writer_notes
  ADD CONSTRAINT note_on_id_fkey
  FOREIGN KEY (note_on_id) REFERENCES objective8.global_identifiers (_id);
