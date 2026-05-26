package com.libratrack.dto.response;
import com.libratrack.enums.*;
import java.time.LocalDateTime;
public record CopyDTO(Long id,Long bookId,String copyNumber,CopyCondition condition,CopyStatus status,LocalDateTime addedAt){}
