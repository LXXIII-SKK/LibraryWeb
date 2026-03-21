package com.example.library.catalog;

import java.util.List;

public record BookFilterOptionsResponse(List<String> categories, List<String> tags) {
}
