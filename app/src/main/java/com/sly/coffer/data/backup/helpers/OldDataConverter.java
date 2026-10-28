package com.sly.coffer.data.backup.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface OldDataConverter<M> {
    M convertOldDataBeforeRoom(String json) throws JsonProcessingException;
    M convertOldDataBeforeExtra(String json) throws JsonProcessingException;
}
