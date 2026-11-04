package com.cityblock.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.cityblock.backend.entity.enums.SpaceType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Property.class, name = "PROPERTY"),
    @JsonSubTypes.Type(value = MetroStation.class, name = "METRO_STATION"),
    @JsonSubTypes.Type(value = Utility.class, name = "UTILITY"),
    @JsonSubTypes.Type(value = TaxSpace.class, name = "TAX"),
    @JsonSubTypes.Type(value = Space.class, name = "CHANCE"),
    @JsonSubTypes.Type(value = Space.class, name = "TROUBLE"),
    @JsonSubTypes.Type(value = Space.class, name = "GO_TO_JAIL"),
    @JsonSubTypes.Type(value = Space.class, name = "FREE_PARKING"),
    @JsonSubTypes.Type(value = Space.class, name = "JAIL"),
    @JsonSubTypes.Type(value = Space.class, name = "JAIL_VISIT"),
    @JsonSubTypes.Type(value = Space.class, name = "GO"),
    @JsonSubTypes.Type(value = Space.class, name = "CITY_PARK")
})
public class Space {
    private String id;
    private int position;
    private String name;
    private SpaceType type;
}
