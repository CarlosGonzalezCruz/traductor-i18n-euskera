package es.bilbomatica.traductor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.xml.sax.SAXException;

import es.bilbomatica.test.logic.FileRequestStatus;
import es.bilbomatica.test.logic.I18nResourceFileType;
import es.bilbomatica.traductor.exceptions.FileRequestQueueAtCapacityException;
import es.bilbomatica.traductor.exceptions.InvalidI18nResourceTypeException;
import es.bilbomatica.traductor.exceptions.WrongFormatException;
import es.bilbomatica.traductor.model.FileRequest;
import es.bilbomatica.traductor.model.FileRequestWSInfo;

@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class FileRequestQueueServiceImplTests {

    @Autowired
    private FileRequestQueueService fileRequestQueueService;

    private FileRequest fileRequest1;
    private FileRequest fileRequest2;
    private FileRequest fileRequest3;

    @BeforeEach
    public void setup() throws InvalidI18nResourceTypeException, XPathExpressionException, WrongFormatException, IOException, ParserConfigurationException, SAXException {
        UUID mockId = new UUID(0L, 0L);

        fileRequest1 = FileRequest.create(I18nResourceFileType.AUTO.getName(), "source1.properties", new ByteArrayInputStream("key = value".getBytes()), mockId);

        fileRequest2 = FileRequest.create(I18nResourceFileType.AUTO.getName(), "source2.properties", new ByteArrayInputStream("key = value".getBytes()), mockId);
        fileRequest2.setStatus(FileRequestStatus.DONE);

        fileRequest3 = FileRequest.create(I18nResourceFileType.AUTO.getName(), "source3.properties", new ByteArrayInputStream("key = value".getBytes()), mockId);
    }

    @Test
    public void testAdd() throws FileRequestQueueAtCapacityException {
        fileRequestQueueService.add(fileRequest1);
        assertTrue(fileRequest1.getId().isPresent());
    }

    @Test
    public void testRetrieve() throws FileRequestQueueAtCapacityException {
        fileRequestQueueService.add(fileRequest1);
        FileRequest retrieved = fileRequestQueueService.get(fileRequest1.getId().get());

        assertEquals(retrieved, fileRequest1);
    }

    @Test
    public void testNext_pending() throws FileRequestQueueAtCapacityException {
        fileRequestQueueService.add(fileRequest2);
        fileRequestQueueService.add(fileRequest1);

        Optional<FileRequest> next = fileRequestQueueService.next();

        assertTrue(next.isPresent());
        assertEquals(fileRequest1, next.get());
    }

    @Test
    public void testNext_nonePending() throws FileRequestQueueAtCapacityException {
        fileRequestQueueService.add(fileRequest2);

        Optional<FileRequest> next = fileRequestQueueService.next();

        assertTrue(next.isEmpty());
    }

    @Test
    public void testRemove() throws FileRequestQueueAtCapacityException {
        fileRequestQueueService.add(fileRequest1);
        fileRequestQueueService.remove(fileRequest1.getId().get());

        assertEquals(0, fileRequestQueueService.getAllRequestsInfo().size());
    }

    @Test
    public void testRearrange() throws FileRequestQueueAtCapacityException {
        fileRequestQueueService.add(fileRequest1);
        fileRequestQueueService.add(fileRequest2);
        fileRequestQueueService.add(fileRequest3);

        List<UUID> requestIds = new ArrayList<>();
        requestIds.add(fileRequest3.getId().get());
        requestIds.add(fileRequest1.getId().get());
        requestIds.add(fileRequest2.getId().get());

        fileRequestQueueService.rearrange(requestIds);

        List<FileRequestWSInfo> allRequests = fileRequestQueueService.getAllRequestsInfo();
        
        assertEquals(fileRequest3.getId(), allRequests.get(0).getId());
        assertEquals(fileRequest1.getId(), allRequests.get(1).getId());
        assertEquals(fileRequest2.getId(), allRequests.get(2).getId());
    }

    @Test
    public void testRearrange_invalidIds() throws FileRequestQueueAtCapacityException {
        fileRequestQueueService.add(fileRequest1);
        fileRequestQueueService.add(fileRequest2);

        List<UUID> requestIds = new ArrayList<>();
        requestIds.add(UUID.randomUUID());
        requestIds.add(fileRequest1.getId().get());

        assertThrows(IllegalArgumentException.class, () -> fileRequestQueueService.rearrange(requestIds));
    }
}
