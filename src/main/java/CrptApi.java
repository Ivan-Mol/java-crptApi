import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final AtomicInteger requestCounter;
    private Date lastResetTime;
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Object monitor;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.httpClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
        this.requestCounter = new AtomicInteger(0);
        this.lastResetTime = new Date();
        this.monitor = new Object();
    }

    @SneakyThrows
    public String createDocument(Document document, String signature) {
        checkRequestLimits();
        String json = mapper.writeValueAsString(document);
        String response = post(json, signature);
        requestCounter.incrementAndGet();
        return "create Document response: " + response;
    }

    @SneakyThrows
    private void checkRequestLimits() {
        synchronized (monitor) {
            long currentTime = System.currentTimeMillis();
            long timePassed = currentTime - lastResetTime.getTime();
            if (timePassed >= timeUnit.toMillis(1)) {
                requestCounter.set(0);
                lastResetTime = new Date(currentTime);
            }

            while (requestCounter.get() >= requestLimit) {
                wait(timeUnit.toMillis(1) - timePassed);
                currentTime = System.currentTimeMillis();
                timePassed = currentTime - lastResetTime.getTime();

                if (timePassed >= timeUnit.toMillis(1)) {
                    requestCounter.set(0);
                    lastResetTime = new Date(currentTime);
                }
            }
        }
    }

    @SneakyThrows
    private String post(String json, String signature) {
        HttpPost httpPost = new HttpPost(URL);
        httpPost.setEntity(new StringEntity(json));
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Signature", signature);

        CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        return EntityUtils.toString(responseEntity, "UTF-8");
    }

    @Getter
    @Setter
    @ToString
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Document {

        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {


        Document doc = new Document();
        doc.setDescription(new Description("some partinn"));
        doc.setDoc_id("doc_001");
        doc.setDoc_status("NEW");
        doc.setDoc_type("LP_INTRODUCE_GOODS");
        doc.setImportRequest(true);
        doc.setOwner_inn("123");
        doc.setParticipant_inn("123456");
        doc.setProducer_inn("123123123");
        doc.setProduction_date("2024-01-23");
        doc.setProduction_type("some type");
        doc.setReg_date("2024-01-01");
        doc.setReg_date("2024-01-01");
        doc.setReg_number("444 1223");
        Product product = new Product();
        product.setCertificate_document("certificate.doc");
        product.setCertificate_document_date("2024-01-23");
        product.setCertificate_document_number("123654");
        product.setOwner_inn("1233 2133");
        product.setProducer_inn("1233 4322");
        product.setProduction_date("2024-01-25");
        product.setTnved_code("code");
        product.setTnved_code("tvncode");
        product.setUit_code("uitcode");
        product.setUitu_code("uitu");
        doc.setProducts(List.of(product));
        System.out.println("document: " + doc);

        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 6);
        String response = crptApi.createDocument(doc, "some signature");
        System.out.println(response);
    }
}