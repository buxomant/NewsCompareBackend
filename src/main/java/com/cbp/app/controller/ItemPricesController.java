package com.cbp.app.controller;

import com.cbp.app.model.db.Item;
import com.cbp.app.model.response.ItemPriceResponse;
import com.cbp.app.model.response.ItemPricesResponse;
import com.cbp.app.repository.ItemPriceRepository;
import com.cbp.app.repository.ItemRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
public class ItemPricesController {

    private final ItemPriceRepository itemPriceRepository;
    private final ItemRepository itemRepository;

    public ItemPricesController(
        ItemPriceRepository itemPriceRepository,
        ItemRepository itemRepository
    ) {
        this.itemPriceRepository = itemPriceRepository;
        this.itemRepository = itemRepository;
    }

    @RequestMapping(value = "/item-prices/{itemId}", method = RequestMethod.GET)
    public ItemPricesResponse getItemPrices(@PathVariable int itemId) {
        Optional<Item> item = itemRepository.findById(itemId);
        if (item.isPresent()) {
            List<ItemPriceResponse> itemPrices = itemPriceRepository
                .findAllByItemId(item.get().getItemId())
                .stream()
                .map(ItemPriceResponse::new)
                .collect(Collectors.toList());
            return new ItemPricesResponse(itemPrices);
        } else {
            return new ItemPricesResponse(Collections.emptyList());
        }
    }
}