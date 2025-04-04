
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
    public Map<String, String> updateTodo(@PathVariable String id, @RequestBody Map<String, Object> todo) {
        try {
            // Validate input
            if (todo.get("title") == null || todo.get("completed") == null) {
                throw new IllegalArgumentException("Title and completed status are required");
            }

            String title = (String) todo.get("title");
            Boolean completed = (Boolean) todo.get("completed");

            // Create key for the item
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());

            // Create update expression and attribute values
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#T", "title");
            expressionAttributeNames.put("#C", "completed");

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":title", AttributeValue.builder().s(title).build());
            expressionAttributeValues.put(":completed", AttributeValue.builder().bool(completed).build());

            // Update the item
            UpdateItemResponse updateResult = dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET #T = :title, #C = :completed")
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .returnValues(ReturnValue.ALL_NEW)
                    .build());

            // Create response
            Map<String, String> response = new HashMap<>();
            response.put("id", id);
            response.put("title", title);
            response.put("completed", completed.toString());
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to update todo: " + e.getMessage());
        }
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
