package com.example.assistant;

import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

@SpringBootApplication
public class AssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }


    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore).build();
    }

    @Bean
    MessageChatMemoryAdvisor chatMemoryAdvisor(DataSource dataSource) {
        // 1. 使用 builder 建立 JdbcChatMemoryRepository，dialect 自動偵測
        JdbcChatMemoryRepository jdbcRepo = JdbcChatMemoryRepository.builder()
                .dataSource(dataSource)
                .dialect(JdbcChatMemoryRepositoryDialect.from(dataSource))
                .build();

        // 2. 使用 builder 建立 MessageWindowChatMemory（預設 20 則，可自訂）
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcRepo)
                .maxMessages(100)
                .build();

        // 3. 使用 builder 建立 MessageChatMemoryAdvisor
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

}

@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;

    AssistantController(
            ToolCallbackProvider toolCallbackProvider,
            DogRepository repository,
            VectorStore vectorStore,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor,
            ChatClient.Builder ai) {

        if (false) {
            repository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                        dog.id(), dog.name(), dog.description()
                ));
                vectorStore.add(List.of(dogument));
            });
        }
        var system = """
                
                You are an AI powered assistant to help people adopt a dog from the adoptions agency named Pooch Palace with locations in Istanbul, Seoul, Tokyo, Singapore, Paris, Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs availables will be presented below. If there is no information, then return a polite response suggesting wes don't have any dogs available.
                
                If somebody asks for a time to pick up the dog, don't ask other questions: simply provide a time by consulting the tools you have available.
                
                """;
        this.ai = ai
                .defaultAdvisors(questionAnswerAdvisor, messageChatMemoryAdvisor)
                .defaultSystem(system)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    @GetMapping("/{user}/ask")
    String ask(@PathVariable String user, @RequestParam String question) {
        return this.ai
                .prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user))
                .call()
                .content();
    }

}


record DogAdoptionSuggestion(int id, String name, String description) {
}


interface DogRepository extends ListCrudRepository<Dog, Integer> {

    Collection<Dog> findByName(String name);
}

// look mom, no Lombok!
record Dog(@Id int id, String owner, String name, String description) {
}