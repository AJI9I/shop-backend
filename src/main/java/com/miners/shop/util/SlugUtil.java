package com.miners.shop.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Утилита для генерации SEO-friendly URL (slug) из текста
 */
public class SlugUtil {
    
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDASHES = Pattern.compile("(^-|-$)");
    
    /**
     * Генерирует slug из входной строки
     * Транслитерирует кириллицу в латиницу, удаляет спецсимволы, приводит к нижнему регистру
     * 
     * @param input Входная строка
     * @return Slug (например: "antminer-s19j-pro-104th")
     */
    public static String generateSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Транслитерация кириллицы в латиницу
        String transliterated = transliterate(input);
        
        // Нормализация и очистка
        String nowhitespace = WHITESPACE.matcher(transliterated).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = EDGESDASHES.matcher(slug).replaceAll("");
        
        // Удаляем множественные дефисы
        slug = slug.replaceAll("-+", "-");
        
        return slug.toLowerCase(Locale.ENGLISH);
    }
    
    /**
     * Транслитерация кириллицы в латиницу
     * 
     * @param text Текст на русском языке
     * @return Транслитерированный текст
     */
    private static String transliterate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            String transliterated = transliterateChar(c);
            result.append(transliterated);
        }
        return result.toString();
    }
    
    /**
     * Транслитерация одного символа
     */
    private static String transliterateChar(char c) {
        return switch (c) {
            case 'А', 'а' -> "a";
            case 'Б', 'б' -> "b";
            case 'В', 'в' -> "v";
            case 'Г', 'г' -> "g";
            case 'Д', 'д' -> "d";
            case 'Е', 'е' -> "e";
            case 'Ё', 'ё' -> "e";
            case 'Ж', 'ж' -> "zh";
            case 'З', 'з' -> "z";
            case 'И', 'и' -> "i";
            case 'Й', 'й' -> "y";
            case 'К', 'к' -> "k";
            case 'Л', 'л' -> "l";
            case 'М', 'м' -> "m";
            case 'Н', 'н' -> "n";
            case 'О', 'о' -> "o";
            case 'П', 'п' -> "p";
            case 'Р', 'р' -> "r";
            case 'С', 'с' -> "s";
            case 'Т', 'т' -> "t";
            case 'У', 'у' -> "u";
            case 'Ф', 'ф' -> "f";
            case 'Х', 'х' -> "h";
            case 'Ц', 'ц' -> "ts";
            case 'Ч', 'ч' -> "ch";
            case 'Ш', 'ш' -> "sh";
            case 'Щ', 'щ' -> "sch";
            case 'Ъ', 'ъ' -> "";
            case 'Ы', 'ы' -> "y";
            case 'Ь', 'ь' -> "";
            case 'Э', 'э' -> "e";
            case 'Ю', 'ю' -> "yu";
            case 'Я', 'я' -> "ya";
            default -> String.valueOf(c);
        };
    }
}





