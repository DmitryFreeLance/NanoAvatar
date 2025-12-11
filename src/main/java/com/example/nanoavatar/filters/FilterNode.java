package com.example.nanoavatar.filters;

import java.util.ArrayList;
import java.util.List;

/**
 * Узел меню настроек.
 * Используется и для корня, и для категорий, и для отдельных опций.
 */
public class FilterNode {
    private final String id;          // уникальный ID узла
    private final String title;       // текст кнопки (с эмодзи)
    private final String description; // текст описания опции/категории
    private final String promptPart;  // доп. инструкции для системного промпта
    private final String parentId;
    private final List<String> childrenIds = new ArrayList<>();

    public FilterNode(String id, String title, String description, String promptPart, String parentId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.promptPart = promptPart;
        this.parentId = parentId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPromptPart() { return promptPart; }
    public String getParentId() { return parentId; }
    public List<String> getChildrenIds() { return childrenIds; }

    public boolean isLeaf() { return childrenIds.isEmpty(); }

    public void addChild(String childId) {
        childrenIds.add(childId);
    }
}