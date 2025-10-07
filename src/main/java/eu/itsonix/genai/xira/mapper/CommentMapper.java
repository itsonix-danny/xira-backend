package eu.itsonix.genai.xira.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import eu.itsonix.genai.xira.jpa.entity.IssueComment;
import eu.itsonix.genai.xira.web.model.CommentResponse;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toCommentResponse(final IssueComment comment) {
        return new CommentResponse()
                .id(UUID.fromString(comment.getId()))
                .author(UserMapper.toUserResponse(comment.getAuthor()))
                .content(comment.getContent())
                .createdAt(OffsetDateTime.ofInstant(comment.getCreatedAt(), ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.ofInstant(comment.getUpdatedAt(), ZoneOffset.UTC));
    }
}
