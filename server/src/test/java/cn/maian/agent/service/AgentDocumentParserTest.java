package cn.maian.agent.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class AgentDocumentParserTest {
    MockMultipartFile file(String name, byte[] bytes) { return new MockMultipartFile("file", name, "application/octet-stream", bytes); }
    @Test void extractsRealPdfAndUtf8Text() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var pdf = new PDDocument()) {
            var page = new PDPage(); pdf.addPage(page);
            try (var stream = new PDPageContentStream(pdf, page)) {
                stream.beginText(); stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12); stream.newLineAtOffset(30, 700); stream.showText("agent file import"); stream.endText();
            }
            pdf.save(out);
        }
        assertThat(AgentDocumentParser.validate(file("test.pdf", out.toByteArray())).text()).contains("agent file import");
        assertThat(AgentDocumentParser.validate(file("体检.txt", "测试体检中心\n血压 120/80 mmHg".getBytes(StandardCharsets.UTF_8))).text()).contains("120/80");
    }
    @Test void extractsDocxParagraphsAndRejectsExternalEntities() throws Exception {
        String xml = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>科普正文</w:t></w:r></w:p></w:body></w:document>";
        assertThat(AgentDocumentParser.validate(file("稿件.docx", zip(xml))).text()).isEqualTo("科普正文");
        String bad = "<!DOCTYPE data [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>" + xml.replace("科普正文", "&x;");
        assertThatIllegalArgumentException().isThrownBy(() -> AgentDocumentParser.validate(file("unsafe.docx", zip(bad))));
    }
    @Test void rejectsRenamedBinaryMalformedPdfAndOversizedExtractedText() {
        assertThatIllegalArgumentException().isThrownBy(() -> AgentDocumentParser.validate(file("fake.pdf", "hello".getBytes())));
        assertThatIllegalArgumentException().isThrownBy(() -> AgentDocumentParser.validate(file("binary.txt", new byte[]{(byte)0xff,0})));
        assertThatIllegalArgumentException().isThrownBy(() -> AgentDocumentParser.validate(file("large.txt", "a".repeat(200001).getBytes())));
        assertThatIllegalArgumentException().isThrownBy(() -> AgentDocumentParser.validate(file("run.exe", "hello".getBytes())));
    }
    byte[] zip(String xml) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) { zip.putNextEntry(new ZipEntry("word/document.xml")); zip.write(xml.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
        return out.toByteArray();
    }
}
