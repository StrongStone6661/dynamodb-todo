
package com.example.dynamodb_todo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "*")
public class TodoController {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Value("${dynamodb.tableName}")
    private String tableName;

    @PostMapping
    public Map<String, String> createTodo(@RequestBody Map<String, String> todo) {
        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("title", AttributeValue.builder().s(todo.get("title")).build());
        item.put("completed", AttributeValue.builder().bool(false).build());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());

        Map<String, String> response = new HashMap<>();
        response.put("id", id);
        response.put("title", todo.get("title"));
        response.put("completed", "false");
        return response;
    }

    @GetMapping
    public List<Map<String, Object>> getTodos() {
        ScanResponse response = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(tableName)
                .build());

        List<Map<String, Object>> todos = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            Map<String, Object> todo = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                String key = entry.getKey();
                AttributeValue value = entry.getValue();
                if (value.s() != null) {
                    todo.put(key, value.s()); // Handle string values
                } else if (value.bool() != null) {
                    todo.put(key, value.bool()); // Handle boolean values
                }
            }
            todos.add(todo);
        }
        return todos;
    }

    @PutMapping("/{id}")
    public Map<String, String> updateTodo(@PathVariable String id, @RequestBody Map<String, String> todo) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("title", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(todo.get("title")).build())
                .action(AttributeAction.PUT)
                .build());
        updates.put("completed", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().bool(Boolean.parseBoolean(todo.get("completed"))).build())
                .action(AttributeAction.PUT)
                .build());

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .attributeUpdates(updates)
                .build());

        Map<String, String> response = new HashMap<>();
        response.put("id", id);
        response.put("title", todo.get("title"));
        response.put("completed", todo.get("completed"));
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteTodo(@PathVariable String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Todo deleted successfully");
        response.put("id", id);
        return response;
    }
}
