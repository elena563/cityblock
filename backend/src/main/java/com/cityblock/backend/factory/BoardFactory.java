package com.cityblock.backend.factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cityblock.backend.entity.Board;
import com.cityblock.backend.entity.Space;
import com.cityblock.backend.entity.Utility;
import com.cityblock.backend.entity.Property;
import com.cityblock.backend.entity.Group;
import com.cityblock.backend.entity.MetroStation;
import com.cityblock.backend.entity.TaxSpace;
import com.cityblock.backend.entity.enums.SpaceType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class BoardFactory {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void initBoard(Board board) throws IOException {
        // groups
        InputStream groupsIs = getClass().getResourceAsStream("/groups-config.json");
        List<Group> groups = objectMapper.readValue(groupsIs, new TypeReference<List<Group>>() {});
        
        Map<String, Group> groupsByName = groups.stream()
            .collect(Collectors.toMap(Group::getName, g -> g));

        // spaces
        List<Space> spaces = new ArrayList<>();
        
        // board-config
        spaces.addAll(loadSpaces("/board-config.json", groupsByName));
        spaces.addAll(loadSpaces("/properties-config.json", groupsByName));
        spaces.addAll(loadSpaces("/stations-config.json", groupsByName));
        spaces.addAll(loadSpaces("/utilities-config.json", groupsByName));

        spaces.sort(Comparator.comparingInt(Space::getPosition));
        board.setSpaces(spaces);
        
        board.setPieces(new ArrayList<>());
    }

    private List<Space> loadSpaces(String resourcePath, Map<String, Group> groupsByName) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        JsonNode nodes = objectMapper.readTree(is);
        if (nodes == null) {
            return new ArrayList<>();
        }
        
        List<Space> spaces = new ArrayList<>();
        for (JsonNode node : nodes) {
            SpaceType type = SpaceType.valueOf(node.get("spaceType").asText());
            int position = node.get("position").asInt();
            String name = node.get("name").asText();
            
            Space space = switch (type) {
                case PROPERTY -> {
                    Property prop = new Property();
                    prop.setPosition(position);
                    prop.setName(name);
                    prop.setType(SpaceType.PROPERTY);
                    prop.setGroup(groupsByName.get(node.get("groupName").asText()));
                    prop.setBaseRent(node.get("baseRent").asInt());
                    prop.setPrice(node.get("price").asInt());
                    yield prop;
                }
                case METRO_STATION -> {
                    MetroStation ms = new MetroStation();
                    ms.setPosition(position);
                    ms.setName(name);
                    ms.setType(SpaceType.METRO_STATION);
                    ms.setColor(node.get("color").asText());
                    ms.setGroup(groupsByName.get(node.get("groupName").asText()));
                    ms.setBaseRent(node.get("baseRent").asInt());
                    ms.setPrice(node.get("price").asInt());
                    yield ms;
                }
                case UTILITY -> {
                    Utility utility = new Utility();
                    utility.setPosition(position);
                    utility.setName(name);
                    utility.setType(SpaceType.UTILITY);
                    utility.setGroup(groupsByName.get(node.get("groupName").asText()));
                    utility.setBaseRent(node.get("baseRent").asInt());
                    utility.setPrice(node.get("price").asInt());
                    yield utility;
                }
                case TAX -> {
                    TaxSpace tax = new TaxSpace();
                    tax.setPosition(position);
                    tax.setName(name);
                    tax.setType(SpaceType.TAX);
                    tax.setTaxAmount(node.get("taxAmount").asInt());
                    yield tax;
                }
                default -> {
                    Space defaultSpace = new Space();
                    defaultSpace.setPosition(position);
                    defaultSpace.setName(name);
                    defaultSpace.setType(type);
                    yield defaultSpace;
                }
            };
            spaces.add(space);
        }
        return spaces;
    }
}
