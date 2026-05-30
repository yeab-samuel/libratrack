package com.libratrack.service;
import com.libratrack.dto.request.WaiveRequest;
import com.libratrack.dto.response.*;
import com.libratrack.entity.*;
import com.libratrack.enums.FineStatus;
import com.libratrack.exception.ResourceNotFoundException;
import com.libratrack.repository.*;
import org.springframework.security.access.AccessDeniedException;
import com.libratrack.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
@Service @RequiredArgsConstructor
public class FineService {
    private final FineRecordRepository fineRepository;
    private final UserRepository userRepository;
    @Transactional(readOnly=true) public Page<FineDTO> getMyFines(String email,Pageable pageable){User m=userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));return fineRepository.findByMember(m,pageable).map(this::toDTO);}
    @Transactional(readOnly=true) public Page<FineDTO> getAllFines(Long memberId,FineStatus status,Pageable pageable){return fineRepository.findWithFilters(memberId,status,pageable).map(this::toDTO);}
    @Transactional(readOnly=true) public FineDTO getFineById(Long id,String callerEmail){FineRecord fine=fineRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Fine not found: "+id));User caller=userRepository.findByEmail(callerEmail).orElseThrow(()->new ResourceNotFoundException("User not found"));if((caller.getRole()==Role.STUDENT||caller.getRole()==Role.FACULTY)&&!fine.getMember().getId().equals(caller.getId())){throw new AccessDeniedException("Access denied to fine: "+id);}return toDTO(fine);}
    @Transactional public FineDTO markAsPaid(Long id,String email){FineRecord f=fineRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Fine not found: "+id));User lib=userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));f.setStatus(FineStatus.PAID);f.setPaidAt(LocalDateTime.now());f.setCollectedBy(lib);return toDTO(fineRepository.save(f));}
    @Transactional public FineDTO waiveFine(Long id,WaiveRequest req,String email){FineRecord f=fineRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Fine not found: "+id));User admin=userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));f.setStatus(FineStatus.WAIVED);f.setCollectedBy(admin);f.setWaiveReason(req.reason());return toDTO(fineRepository.save(f));}
    @Transactional(readOnly=true) public FineSummaryDTO getFinesSummary(LocalDate from,LocalDate to){List<FineRecord> fines=fineRepository.findByStatusAndCreatedAtBetween(FineStatus.PAID,from.atStartOfDay(),to.plusDays(1).atStartOfDay());BigDecimal total=fines.stream().map(FineRecord::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);return new FineSummaryDTO(total,fines.size(),from,to);}
    public FineDTO toDTO(FineRecord f){return new FineDTO(f.getId(),f.getLoan().getId(),f.getMember().getId(),f.getMember().getFullName(),f.getLoan().getBookCopy().getBook().getTitle(),f.getAmount(),f.getStatus(),f.getPaidAt(),f.getCollectedBy()!=null?f.getCollectedBy().getId():null,f.getWaiveReason(),f.getCreatedAt());}
}
