package comment.service.service;

import comment.service.feign.client.EventClient;
import comment.service.feign.client.UserClient;
import comment.service.mapper.CommentMapper;
import comment.service.model.Comment;
import comment.service.repository.CommentRepository;
import interaction.api.dto.comment.CommentDto;
import interaction.api.dto.comment.NewCommentDto;
import interaction.api.dto.event.EventFullDto;
import interaction.api.dto.user.UserShortDto;
import interaction.api.enums.EventState;
import interaction.api.exception.ConflictException;
import interaction.api.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings("unused")
@Slf4j
public class CommentServiceImpl implements CommentService {
    CommentRepository commentRepository;
    UserClient userClient;
    EventClient eventClient;
    CommentMapper commentMapper;

    @Override
    public List<CommentDto> findAllByEvent(Long eventId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from > 0 ? from / size : 0, size, Sort.by(Sort.Direction.ASC, "id"));
        return commentRepository.findAllByEvent(pageable, eventId).stream()
                .map(commentMapper::toCommentDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto create(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Создание комментария для события id: {}, пользователем id: {}", eventId, userId);

        EventFullDto event = eventClient.getEvent(eventId);

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException(String.format("Событие с id= %d не опубликовано", eventId));
        }

        UserShortDto user = userClient.getUserById(userId);

        Comment comment = commentMapper.toComment(newCommentDto);
        comment.setEvent(event.getId());
        comment.setAuthorId(user.getId());
        comment.setCreatedOn(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        log.info("Комментарий создан с id: {}", savedComment.getId());

        return commentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto update(Long userId, Long eventId, Long commentId, NewCommentDto updateCommentDto) {
        log.info("Обновление комментария с id: {} пользователем id: {} для события id: {}", commentId, userId, eventId);
        Comment comment = validateCommentForEventAndUser(eventId, commentId, userId);
        comment.setMessage(updateCommentDto.getMessage());
        comment.setUpdatedOn(LocalDateTime.now());
        log.info("Комментарий с id: {} обновлён пользователем id: {}", commentId, userId);
        return commentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long eventId, Long commentId) {
        log.info("Запрос на удаление комментария с id: {} пользователем id: {} для события id: {}", commentId,
                userId, eventId);
        Comment comment = validateCommentForEventAndUser(eventId, commentId, userId);
        commentRepository.delete(comment);
        log.info("Комментарий с id: {} был успешно удален пользователем id: {}", commentId, userId);
    }

    @Override
    @Transactional
    public CommentDto adminUpdate(Long eventId, Long commentId, NewCommentDto updateCommentDto) {
        log.info("Администратор обновляет комментарий с id: {} для события id: {}", commentId, eventId);
        Comment comment = validateCommentForEvent(eventId, commentId);
        comment.setMessage(updateCommentDto.getMessage());
        comment.setUpdatedOn(LocalDateTime.now());
        log.info("Комментарий с id: {} обновлён администратором данными: {}", commentId, updateCommentDto);
        return commentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
    public void adminDelete(Long eventId, Long commentId) {
        log.info("Администратор удаляет комментарий с id: {} для события id: {}", commentId, eventId);
        Comment comment = validateCommentForEvent(eventId, commentId);
        commentRepository.delete(comment);
        log.info("Комментарий с id: {} был успешно удален администратором для события id: {}", commentId, eventId);
    }

    @Override
    public CommentDto findByEventAndCommentId(Long eventId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий id= %d не найден", commentId)));
        if (!Objects.equals(eventId, comment.getId())) {
            throw new ConflictException(String.format("Комментарий id= %d не относится к событию с id= %d",
                    comment.getId(), eventId));
        }
        return commentMapper.toCommentDto(comment);
    }

    private Comment validateCommentForEventAndUser(Long eventId, Long commentId, Long userId) {
        log.info("Валидация комментария с id: {} для события с id: {} и пользователя с id: {}", commentId, eventId,
                userId);
        Comment comment = validateCommentForEvent(eventId, commentId);

        UserShortDto user = userClient.getUserById(userId);

        if (!user.getId().equals(comment.getAuthorId())) {
            throw new ConflictException(String.format("Комментарий id= %d не был создан пользователем с id= %d",
                    comment.getId(), user.getId()));
        }

        log.info("Комментарий с id: {} успешно валидирован для события id: {} и пользователя id: {}", commentId,
                eventId, userId);
        return comment;
    }

    private Comment validateCommentForEvent(Long eventId, Long commentId) {
        log.info("Валидация комментария с id: {} для события с id: {}", commentId, eventId);
        EventFullDto event = eventClient.getEvent(eventId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий id= %d не найден", commentId)));

        if (!Objects.equals(comment.getId(), event.getId())) {
            throw new ConflictException(String.format("Комментарий id= %d не относится к событию id= %d",
                    comment.getId(), event.getId()));
        }
        log.info("Комментарий с id: {} успешно валидирован для события id: {}", commentId, eventId);
        return comment;
    }
}
