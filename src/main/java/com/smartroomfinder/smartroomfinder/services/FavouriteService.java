package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.response.FavouriteResponse;
import com.smartroomfinder.smartroomfinder.entities.Favourites;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.mappers.RoomMapper;
import com.smartroomfinder.smartroomfinder.repositories.FavouriteRepository;
import com.smartroomfinder.smartroomfinder.repositories.RoomRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavouriteService {

    private final FavouriteRepository favouriteRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMapper roomMapper;

    /**
     * Toggle: if not saved → save; if already saved → remove.
     * Returns the new state.
     */
    @Transactional
    public FavouriteResponse toggle(Long roomId, UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        boolean alreadySaved = favouriteRepository.existsByUserAndRoom(user, room);

        if (alreadySaved) {
            favouriteRepository.deleteByUserAndRoom(user, room);
            log.info("Favourite removed - roomId: {}, userId: {}", roomId, userId);
            return FavouriteResponse.builder()
                    .roomId(roomId)
                    .saved(false)
                    .build();
        } else {
            Favourites saved = favouriteRepository.save(
                    Favourites.builder().user(user).room(room).build());
            log.info("Favourite added - roomId: {}, userId: {}", roomId, userId);
            return FavouriteResponse.builder()
                    .favId(saved.getFavId())
                    .roomId(roomId)
                    .saved(true)
                    .createdAt(saved.getCreatedAt())
                    .build();
        }
    }

    /**
     * Paginated list of saved rooms for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<FavouriteResponse> getMyFavourites(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return favouriteRepository.findByUserIdWithRoom(userId, pageable)
                .map(fav -> FavouriteResponse.builder()
                        .favId(fav.getFavId())
                        .roomId(fav.getRoom().getRoomId())
                        .saved(true)
                        .createdAt(fav.getCreatedAt())
                        .room(roomMapper.toResponse(fav.getRoom()))
                        .build());
    }

    /**
     * Returns all saved roomIds for a user — used by search page
     * to pre-mark hearts after loading rooms.
     */
    @Transactional(readOnly = true)
    public Set<Long> getSavedRoomIds(UUID userId) {
        return favouriteRepository.findRoomIdsByUserId(userId);
    }

    /**
     * Check if a single room is saved.
     */
    @Transactional(readOnly = true)
    public boolean isSaved(Long roomId, UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        return favouriteRepository.existsByUserAndRoom(user, room);
    }
}