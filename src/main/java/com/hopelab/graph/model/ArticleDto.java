package com.hopelab.graph.model;

import java.util.List;

public class ArticleDto {
    String id;
    String title;
    Integer year;
    List<String> authors;
    String venue;
    List<String> referenced_works;
}