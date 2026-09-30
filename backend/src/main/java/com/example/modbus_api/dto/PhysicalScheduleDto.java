package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class PhysicalScheduleDto {
    private PhysicalScheduleDto() {}
    @Data @AllArgsConstructor
    public static class Slot {
        private int slotId;
        private int baseAddress;
        private List<Integer> values;
    }
    @Data @AllArgsConstructor
    public static class Snapshot {
        private List<Slot> slots;
        private Integer reportedSlots;
        private long readAt;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Change {
        @NotNull private Integer offset;
        @NotNull private Integer expected;
        @NotNull private Integer value;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Update {
        @NotNull @Size(min=1,max=18) @Valid private List<Change> changes;
        private boolean apply;
    }
    @Data @AllArgsConstructor
    public static class Result {
        private Slot slot;
        private boolean verified;
    }
}
