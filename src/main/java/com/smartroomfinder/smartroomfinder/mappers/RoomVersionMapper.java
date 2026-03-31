package com.smartroomfinder.smartroomfinder.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartroomfinder.smartroomfinder.dto.response.RoomSnapshot;
import com.smartroomfinder.smartroomfinder.dto.response.RoomVersionResponse;
import com.smartroomfinder.smartroomfinder.entities.RoomVersion;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class RoomVersionMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "versionId", source = "versionId")
    @Mapping(target = "roomId", source = "room.roomId")
    @Mapping(target = "landlordId", expression = "java(req.getRequestedBy().getUserId() != null ? req.getRequestedBy().getUserId().toString() : null)")
    @Mapping(target = "landlordName", source = "requestedBy.fullName")
    @Mapping(target = "landlordEmail", source = "requestedBy.email")
    @Mapping(target = "landlordPhone", source = "requestedBy.phoneNumber")
    @Mapping(target = "landlordAvatar", source = "requestedBy.avatarUrl")
    @Mapping(target = "oldData", expression = "java(parseSnapshot(req.getOldData()))")
    @Mapping(target = "newData", expression = "java(parseSnapshot(req.getNewData()))")
    public abstract RoomVersionResponse toResponse(RoomVersion req);

    protected RoomSnapshot parseSnapshot(String json) {
        if (json == null) return null;

        try {
            var m = objectMapper.readValue(json, java.util.Map.class);

            return RoomSnapshot.builder()
                    .title(str(m, "title"))
                    .description(str(m, "description"))
                    .pricePerMonth(toBigDecimal(m, "pricePerMonth"))
                    .depositAmount(toBigDecimal(m, "depositAmount"))
                    .areaSize(toBigDecimal(m, "areaSize"))
                    .capacity(toInteger(m, "capacity"))
                    .roomType(str(m, "roomType"))
                    .furnishLevel(str(m, "furnishLevel"))
                    .availableFrom(str(m, "availableFrom"))
                    .streetAddress(str(m, "streetAddress"))
                    .wardName(str(m, "wardName"))
                    .districtName(str(m, "districtName"))
                    .cityName(str(m, "cityName"))
                    .mediaUrls(toList(m, "mediaUrls"))
                    .amenityNames(toList(m, "amenityNames"))
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    private String str(java.util.Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private java.math.BigDecimal toBigDecimal(java.util.Map<String, Object> m, String key) {
        return m.get(key) != null ? new java.math.BigDecimal(m.get(key).toString()) : null;
    }

    private Integer toInteger(java.util.Map<String, Object> m, String key) {
        return m.get(key) != null ? Integer.parseInt(m.get(key).toString()) : null;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<String> toList(java.util.Map<String, Object> m, String key) {
        return m.get(key) instanceof java.util.List ? (java.util.List<String>) m.get(key) : null;
    }
}