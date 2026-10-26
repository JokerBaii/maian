package cn.maian.agent.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.Locale;
import java.util.zip.ZipInputStream;

/** File contents are untrusted data; this parser never calls a model or executes embedded instructions. */
public final class AgentDocumentParser {
    private AgentDocumentParser() {}
    private static final int LIMIT = 200_000;
    public record Parsed(byte[] bytes, String contentType, String extension, String text, String notice) {}
    public static Parsed validate(MultipartFile file) {
        try { return parse(file.getOriginalFilename(), file.getBytes()); }
        catch (IOException e) { throw new IllegalArgumentException("无法读取文档", e); }
    }
    public static Parsed parse(String filename, byte[] bytes) {
        String name = (filename == null ? "" : filename).toLowerCase(Locale.ROOT);
        try {
            if (bytes.length == 0 || bytes.length > 15 * 1024 * 1024) throw new IllegalArgumentException("文件需为1字节至15MB");
            String type, extension, text;
            if (name.endsWith(".pdf")) {
                if (!new String(bytes, 0, Math.min(5, bytes.length), StandardCharsets.US_ASCII).equals("%PDF-")) throw new IllegalArgumentException("PDF文件内容无效");
                try (var pdf = Loader.loadPDF(bytes)) {
                    if (pdf.isEncrypted() || pdf.getNumberOfPages() > 100) throw new IllegalArgumentException("请使用未加密且不超过100页的PDF");
                    var stripper = new PDFTextStripper();
                    var writer = new BoundedWriter();
                    stripper.writeText(pdf, writer);
                    text = writer.toString();
                }
                type = "application/pdf"; extension = ".pdf";
            } else if (name.endsWith(".docx")) {
                text = word(bytes); type = "application/zip"; extension = ".docx";
            } else if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv")) {
                text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
                if (text.indexOf('\0') >= 0) throw new IllegalArgumentException("文本文件包含无效字符");
                if (text.length() > LIMIT) throw new IllegalArgumentException("文档文字不能超过20万字");
                type = "text/plain"; extension = name.endsWith(".csv") ? ".csv" : name.endsWith(".md") ? ".md" : ".txt";
            } else throw new IllegalArgumentException("文档支持PDF、DOCX、UTF-8 TXT、MD、CSV；检查单图片请选择识别检查单");
            text = text.replace("\uFEFF", "").trim();
            return new Parsed(bytes, type, extension, text, text.isBlank() ? "文件已保存；未提取到可读文字，扫描件需要OCR或补充文字。" : "已读取文档文字，内容仅作为业务资料处理。");
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("无法读取文档，请检查格式、编码或加密状态", e); }
    }
    private static String word(byte[] bytes) throws Exception {
        byte[] xml = null; int entries = 0, total = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (++entries > 500) throw new IllegalArgumentException("DOCX包含过多内容");
                var out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
                while ((count = zip.read(buffer)) >= 0) {
                    total += count;
                    if (total > 20 * 1024 * 1024) throw new IllegalArgumentException("DOCX解压内容过大");
                    if (entry.getName().equals("word/document.xml")) out.write(buffer, 0, count);
                }
                if (entry.getName().equals("word/document.xml")) xml = out.toByteArray();
            }
        }
        if (xml == null) throw new IllegalArgumentException("不是有效的DOCX文档");
        var factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        var paragraphs = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p");
        var result = new StringBuilder();
        for (int i = 0; i < paragraphs.getLength(); i++) {
            result.append(paragraphs.item(i).getTextContent()).append('\n');
            if (result.length() > LIMIT) throw new IllegalArgumentException("文档文字不能超过20万字");
        }
        return result.toString();
    }
    private static class BoundedWriter extends Writer {
        final StringBuilder output = new StringBuilder();
        public void write(char[] value, int offset, int length) {
            if (output.length() + length > LIMIT) throw new IllegalArgumentException("文档文字不能超过20万字");
            output.append(value, offset, length);
        }
        public void flush() {} public void close() {}
        public String toString() { return output.toString(); }
    }
}
