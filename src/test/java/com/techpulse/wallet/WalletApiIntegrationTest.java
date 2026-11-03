package com.techpulse.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class WalletApiIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
    String user1,user2,token1,token2;
    @BeforeEach void setup() throws Exception {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        user1="one"+suffix+"@example.com"; user2="two"+suffix+"@example.com";
        register(user1); register(user2); token1=login(user1); token2=login(user2);
    }
    private void register(String email) throws Exception { mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"StrongPass@123\"}" )).andExpect(status().isCreated()); }
    private String login(String email) throws Exception { String body=mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"StrongPass@123\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return mapper.readTree(body).get("token").asText(); }
    @Test void addIsIdempotentAndTransferIsAtomicAtApiLevel() throws Exception {
        String add="{\"amount\":100.00}";
        String first=mvc.perform(post("/wallet/add").header("Authorization","Bearer "+token1).header("Idempotency-Key","add-"+user1).contentType(MediaType.APPLICATION_JSON).content(add)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS")).andReturn().getResponse().getContentAsString();
        String second=mvc.perform(post("/wallet/add").header("Authorization","Bearer "+token1).header("Idempotency-Key","add-"+user1).contentType(MediaType.APPLICATION_JSON).content(add)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(mapper.readTree(first).get("id").asLong(),mapper.readTree(second).get("id").asLong());
        mvc.perform(post("/wallet/transfer").header("Authorization","Bearer "+token1).header("Idempotency-Key","transfer-"+user1).contentType(MediaType.APPLICATION_JSON).content("{\"receiverEmail\":\""+user2+"\",\"amount\":30.00}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        mvc.perform(get("/wallet").header("Authorization","Bearer "+token1)).andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(70.00));
        mvc.perform(get("/wallet").header("Authorization","Bearer "+token2)).andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(30.00));
        mvc.perform(get("/wallet/transactions").header("Authorization","Bearer "+token1)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
    }
    @Test void insufficientBalanceAndValidationAreHandled() throws Exception {
        mvc.perform(post("/wallet/transfer").header("Authorization","Bearer "+token1).header("Idempotency-Key","insufficient-"+user1).contentType(MediaType.APPLICATION_JSON).content("{\"receiverEmail\":\""+user2+"\",\"amount\":50}")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.status").value("FAILED"));
        mvc.perform(post("/wallet/add").header("Authorization","Bearer "+token1).header("Idempotency-Key","bad-"+user1).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-1}")).andExpect(status().isBadRequest());
        mvc.perform(get("/wallet")).andExpect(status().isUnauthorized());
    }
    @Test void adminCanReadAllAndRegularUserCannot() throws Exception {
        String admin=login("admin@wallet.local");
        mvc.perform(get("/admin/wallets").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/admin/transactions").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/wallets").header("Authorization","Bearer "+token1)).andExpect(status().isForbidden());
    }

    @Test void concurrentTransfersNeverOverdrawTheSender() throws Exception {
        mvc.perform(post("/wallet/add").header("Authorization","Bearer "+token1).header("Idempotency-Key","fund-"+user1).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":100}" )).andExpect(status().isOk());
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch ready=new CountDownLatch(2); CountDownLatch start=new CountDownLatch(1);
        Callable<Integer> transferA=()->concurrentTransfer("race-a-"+user1,ready,start);
        Callable<Integer> transferB=()->concurrentTransfer("race-b-"+user1,ready,start);
        Future<Integer> a=pool.submit(transferA),b=pool.submit(transferB); ready.await();start.countDown();
        int statusA=a.get(15,TimeUnit.SECONDS),statusB=b.get(15,TimeUnit.SECONDS);pool.shutdownNow();
        Assertions.assertTrue(statusA==200||statusA==422||statusA==409);
        Assertions.assertTrue(statusB==200||statusB==422||statusB==409);
        Assertions.assertTrue(statusA==200||statusB==200,"At least one transfer should succeed");
        mvc.perform(get("/wallet").header("Authorization","Bearer "+token1)).andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(20.00));
    }
    private int concurrentTransfer(String key,CountDownLatch ready,CountDownLatch start) throws Exception {
        ready.countDown();start.await();
        return mvc.perform(post("/wallet/transfer").header("Authorization","Bearer "+token1).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content("{\"receiverEmail\":\""+user2+"\",\"amount\":80}")).andReturn().getResponse().getStatus();
    }

    @Test void duplicateRegistrationIsRejected() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+user1+"\",\"password\":\"StrongPass@123\"}")).andExpect(status().isBadRequest());
    }
}
