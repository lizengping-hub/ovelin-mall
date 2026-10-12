package com.ovelin.mall.user.domain.model.valueobject;

import com.ovelin.mall.user.domain.exception.InvalidArgumentException;
import lombok.Getter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@Getter
public class Avatar {
    private static final int MAX_LENGTH = 500;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final String url;

    private Avatar(String url) {
        this.url = url;
    }

    public static Avatar from(String url) {
        String validatedUrl = validate(url);
        return new Avatar(validatedUrl);
    }

    public static Avatar reconstitute(String url) {
        if (url == null || url.isBlank()) {
           throw new InvalidArgumentException("Avatar url cannot be null or blank");
        }
        return new Avatar(url);
    }
    // 提供一个默认头像的静态工厂，避免到处写魔法字符串
    public static Avatar defaultAvatar() {
        return new Avatar("https://cdn.ovelin.com/default-avatar.png");
    }


    private static String validate(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidArgumentException("Avatar url cannot be null or blank");
        }
        url = url.trim();
        if (url.length() > MAX_LENGTH) {
            throw new InvalidArgumentException(
                    "Avatar url too long (max " + MAX_LENGTH + " chars): " + url);
        }
        String lowerUrl = url.toLowerCase();
        URI uri;
        try {
            uri = new URI(lowerUrl);
        } catch (URISyntaxException e) {
            throw new InvalidArgumentException("Invalid avatar url: " + url);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new InvalidArgumentException("Avatar url must be http/https: " + url);
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidArgumentException("Avatar url must have a valid host: " + url);
        }
        String path = uri.getPath();

        boolean hasValidExtension = ALLOWED_EXTENSIONS.stream().anyMatch(path::endsWith);
        if (!hasValidExtension) {
            throw new InvalidArgumentException(
                    "Avatar url must point to an image file (" + ALLOWED_EXTENSIONS + "): " + url);
        }
        return lowerUrl;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Avatar other)) return false;
        return url.equals(other.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
    @Override
    public String toString() {
        return "Avatar{" +
                "url='" + url + '\'' +
                '}';
    }
}