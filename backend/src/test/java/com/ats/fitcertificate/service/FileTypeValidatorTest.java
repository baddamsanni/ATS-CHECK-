package com.ats.fitcertificate.service;

import com.ats.fitcertificate.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileTypeValidatorTest {

    @Test
    void acceptsPdfHeader() {
        byte[] pdf = "%PDF-1.7\n...".getBytes();
        assertDoesNotThrow(() ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(pdf)));
    }

    @Test
    void acceptsDocxZipHeader() {
        // PK\x03\x04
        byte[] docx = {0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00};
        assertDoesNotThrow(() ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(docx)));
    }

    @Test
    void acceptsEmptyZipEndOfArchiveHeader() {
        // PK\x05\x06 (empty archive)
        byte[] zip = {0x50, 0x4B, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00};
        assertDoesNotThrow(() ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(zip)));
    }

    @Test
    void acceptsLegacyDocOle2Header() {
        // D0 CF 11 E0
        byte[] doc = {(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, 0, 0, 0, 0};
        assertDoesNotThrow(() ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(doc)));
    }

    @Test
    void rejectsPlainTextEvenIfNamedPdf() {
        byte[] text = "this is just plain text".getBytes();
        assertThrows(UnsupportedFileTypeException.class, () ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(text)));
    }

    @Test
    void rejectsPngImage() {
        // PNG signature
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThrows(UnsupportedFileTypeException.class, () ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(png)));
    }

    @Test
    void rejectsJpegImage() {
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        assertThrows(UnsupportedFileTypeException.class, () ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(jpg)));
    }

    @Test
    void rejectsTooSmallFile() {
        byte[] tiny = {0x25, 0x50};
        assertThrows(UnsupportedFileTypeException.class, () ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(tiny)));
    }

    @Test
    void rejectsEmptyStream() {
        assertThrows(UnsupportedFileTypeException.class, () ->
                FileTypeValidator.validatePdfOrDocx(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    void streamIsResetAfterValidation() throws IOException {
        byte[] pdf = "%PDF-1.7\nrest of file".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(pdf);
        FileTypeValidator.validatePdfOrDocx(in);
        // After validation the stream should be reset to the start so the
        // parser can read the full content.
        org.junit.jupiter.api.Assertions.assertEquals(0, in.read(new byte[0]));
        byte[] all = in.readAllBytes();
        org.junit.jupiter.api.Assertions.assertArrayEquals(pdf, all);
    }
}
