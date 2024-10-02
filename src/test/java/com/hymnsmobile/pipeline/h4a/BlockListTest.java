package com.hymnsmobile.pipeline.h4a;

import com.hymnsmobile.pipeline.h4a.models.H4aKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class BlockListTest {

  @Mock private Converter converter;

  private List<String> miscBlockList;
  private List<String> nonExistentRelatedSongs;
  private BlockList target;

  @BeforeEach
  void setUp() {
    this.miscBlockList = new ArrayList<>();
    this.nonExistentRelatedSongs = new ArrayList<>();
    this.target = new BlockList(converter, miscBlockList, nonExistentRelatedSongs);
  }

  @Test
  void blockStatus__blocked__returnsBlocked__removeFromList() {
    miscBlockList.add("song1");

    assertThat(miscBlockList).isNotEmpty();
    assertThat(target.blockStatus("song1")).isEqualTo(BlockList.BlockStatus.BLOCKED);
    assertThat(miscBlockList).isEmpty();
  }

  @Test
  void blockStatus__nonExistent__returnsBlocked__removeFromList() {
    nonExistentRelatedSongs.add("song1");

    assertThat(nonExistentRelatedSongs).isNotEmpty();
    assertThat(target.blockStatus("song1")).isEqualTo(BlockList.BlockStatus.NON_EXISTENT);
    assertThat(nonExistentRelatedSongs).isEmpty();
  }

  @Test
  void blockStatus__unparseable__returnsUnparseable() {
    assertThat(target.blockStatus("12")).isEqualTo(BlockList.BlockStatus.UNPARSEABLE);
  }

  @Test
  void chineseEndsWithC__returnsNonExistent() {
    doReturn(Optional.of(H4aKey.newBuilder().setType("C").setNumber("123c").build()))
        .when(converter).toKey("C12c");
    assertThat(target.blockStatus("C12c")).isEqualTo(BlockList.BlockStatus.NON_EXISTENT);
  }

  @Test
  void unknownR__returnsNonExistent() {
    doReturn(Optional.of(H4aKey.newBuilder().setType("R").setNumber("13").build()))
        .when(converter).toKey("R13");
    assertThat(target.blockStatus("R13")).isEqualTo(BlockList.BlockStatus.NON_EXISTENT);
  }

  @Test
  void unknownLB__returnsNonExistent() {
    doReturn(Optional.of(H4aKey.newBuilder().setType("LB").setNumber("13").build()))
        .when(converter).toKey("LB13");
    assertThat(target.blockStatus("LB13")).isEqualTo(BlockList.BlockStatus.NON_EXISTENT);
  }

  @Test
  void tagalogLarge__returnsNonExistent() {
    doReturn(Optional.of(H4aKey.newBuilder().setType("T").setNumber("1361").build()))
        .when(converter).toKey("T1361");
    assertThat(target.blockStatus("T1361")).isEqualTo(BlockList.BlockStatus.BLOCKED);
  }

  @Test
  void normalSong__returnsOk() {
    doReturn(Optional.of(H4aKey.newBuilder().setType("E").setNumber("1151").build()))
        .when(converter).toKey("E1151");
    assertThat(target.blockStatus("E1151")).isEqualTo(BlockList.BlockStatus.OK);
  }

  @Test
  void items__returnsConcatenatedList() {
    miscBlockList.add("song1");
    miscBlockList.add("song2");
    nonExistentRelatedSongs.add("song3");
    miscBlockList.add("song4");
    assertThat(target.items()).containsExactly("song1", "song2", "song4", "song3");
  }
}
