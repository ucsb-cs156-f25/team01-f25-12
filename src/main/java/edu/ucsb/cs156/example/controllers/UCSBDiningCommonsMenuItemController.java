package edu.ucsb.cs156.example.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.example.entities.UCSBDiningCommonsMenuItem;
import edu.ucsb.cs156.example.errors.EntityNotFoundException;
import edu.ucsb.cs156.example.repositories.UCSBDiningCommonsMenuItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "UCSBDiningCommonsMenuItemController")
@RequestMapping("/api/ucsbdiningcommonsmenuitem")
@RestController
@Slf4j
public class UCSBDiningCommonsMenuItemController extends ApiController {
  @Autowired UCSBDiningCommonsMenuItemRepository UCSBDiningCommonsMenuItemRepository;

  /**
   * List all UCSB Menu Items
   *
   * @return an iterable of UCSBItem
   */
  @Operation(summary = "List all dining commons items")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/all")
  public Iterable<UCSBDiningCommonsMenuItem> allUCSBDiningCommonsMenuItems() {
    Iterable<UCSBDiningCommonsMenuItem> menuItems = UCSBDiningCommonsMenuItemRepository.findAll();
    return menuItems;
  }

  /**
   * Create a new item
   *
   * @param diningCommonsCode the code for the dining commons
   * @param name the name of the item
   * @param station the station
   * @return the saved item
   */
  @Operation(summary = "Create a new item")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/post")
  public UCSBDiningCommonsMenuItem postUCSBDiningCommonsMenuItem(
      @Parameter(name = "diningCommonsCode") @RequestParam String diningCommonsCode,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "station") @RequestParam String station)
      throws JsonProcessingException {

    // For an explanation of @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    // See: https://www.baeldung.com/spring-date-parameters

    UCSBDiningCommonsMenuItem UCSBDiningCommonsMenuItem = new UCSBDiningCommonsMenuItem();
    UCSBDiningCommonsMenuItem.setDiningCommonsCode(diningCommonsCode);
    UCSBDiningCommonsMenuItem.setName(name);
    UCSBDiningCommonsMenuItem.setStation(station);

    UCSBDiningCommonsMenuItem savedUCSBDiningCommonsMenuItem =
        UCSBDiningCommonsMenuItemRepository.save(UCSBDiningCommonsMenuItem);

    return savedUCSBDiningCommonsMenuItem;
  }

  @Operation(summary = "Get a single item by id")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("")
  public UCSBDiningCommonsMenuItem getById(@Parameter(name = "id") @RequestParam Long id) {
    UCSBDiningCommonsMenuItem menuItem =
        UCSBDiningCommonsMenuItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(UCSBDiningCommonsMenuItem.class, id));

    return menuItem;
  }
}
