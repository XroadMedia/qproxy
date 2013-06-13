package tv.xrm.qproxy;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class RequestQueueTest {

    private RequestQueue q;
    private static final int CAPACITY = 1024;

    @Before
    public void setup() {
        MetricRegistry metricRegistryMock = mock(MetricRegistry.class);
        RequestStorage requestStorage = new InMemoryStorage();
        q = new RequestQueue("test", requestStorage, metricRegistryMock, CAPACITY, 300);
    }

    @Test
    public void enqueuesWithoutComplaint() throws IOException {
        q.enqueue(TestDataFactory.generateRequest());
    }

    @Test
    public void getsEnqueued() throws IOException, InterruptedException {
        Request r = q.enqueue(TestDataFactory.generateRequest());
        Request taken = q.take();
        assertEquals(r, taken);
    }

    @Test
    public void getsRequeuedWithDelay() throws IOException, InterruptedException {
        Request r = q.enqueue(TestDataFactory.generateRequest());
        q.take();
        q.requeue(r, 500);
        Request taken = q.take();
        assertEquals(r, taken);
    }

    @Test
    public void getsRequeuedWithoutDelay() throws IOException, InterruptedException {
        Request r = q.enqueue(TestDataFactory.generateRequest());
        q.take();
        q.requeue(r, 0);
        Request taken = q.take();
        assertEquals(r, taken);
    }

    @Test(expected = RequestQueue.RequestQueueException.class)
    public void barfsWhenFull() throws IOException {
        for (int i = 0; i <= CAPACITY; i++) {
            q.enqueue(TestDataFactory.generateRequest());
        }
    }

}
