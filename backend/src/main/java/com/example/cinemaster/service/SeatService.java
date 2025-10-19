package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.BulkSeatRequest;
import com.example.cinemaster.dto.request.BulkSeatUpdateRequest;
import com.example.cinemaster.dto.request.SeatRequest;
import com.example.cinemaster.dto.response.SeatResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.entity.Seat;
import com.example.cinemaster.entity.SeatType;
import com.example.cinemaster.mapper.SeatMapper;
import com.example.cinemaster.repository.AuditoriumRepository;
import com.example.cinemaster.repository.SeatRepository;
import com.example.cinemaster.repository.SeatTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final SeatMapper seatMapper;

    // 1️⃣ READ ALL
    public List<SeatResponse> getAllSeats() {
        return seatRepository.findAll()
                .stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 2️⃣ READ ONE
    public SeatResponse getSeatById(Integer id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ghế không tìm thấy với ID: " + id));
        return seatMapper.toResponse(seat);
    }

    // 3️⃣ CREATE
    public SeatResponse createSeat(SeatRequest request) {
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tìm thấy với ID: " + request.getAuditoriumID()));
        SeatType seatType = seatTypeRepository.findById(request.getTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế không tìm thấy với ID: " + request.getTypeID()));

        Seat seat = seatMapper.toEntity(request, auditorium, seatType);
        Seat saved = seatRepository.save(seat);
        return seatMapper.toResponse(saved);
    }

    // 4️⃣ UPDATE
    public SeatResponse updateSeat(Integer id, SeatRequest request) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ghế không tìm thấy với ID: " + id));

        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tìm thấy với ID: " + request.getAuditoriumID()));
        SeatType seatType = seatTypeRepository.findById(request.getTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế không tìm thấy với ID: " + request.getTypeID()));

        seatMapper.updateEntityFromRequest(request, auditorium, seatType, seat);
        Seat updated = seatRepository.save(seat);
        return seatMapper.toResponse(updated);
    }

    // 5️⃣ DELETE
    public void deleteSeat(Integer id) {
        if (!seatRepository.existsById(id))
            throw new EntityNotFoundException("Ghế không tìm thấy với ID: " + id);
        seatRepository.deleteById(id);
    }

    // 6️⃣ CREATE BULK
    @Transactional
    public List<SeatResponse> createBulkSeats(BulkSeatRequest request) {
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tồn tại"));
        SeatType seatType = seatTypeRepository.findById(request.getTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế không tồn tại"));

        List<Seat> newSeats = new ArrayList<>();
        char startRow = request.getStartRowChar().charAt(0);

        for (int r = 0; r < request.getRowCount(); r++) {
            String seatRow = String.valueOf((char) (startRow + r));
            for (int c = 1; c <= request.getColumnCount(); c++) {
                newSeats.add(Seat.builder()
                        .auditorium(auditorium)
                        .seatType(seatType)
                        .seatRow(seatRow)
                        .columnNumber(c)
                        .seatNumber(String.valueOf(c)) // ❗ chỉ số, KHÔNG nối row ở đây
                        .status(Seat.SeatStatus.AVAILABLE)
                        .build());
            }
        }
        List<Seat> savedSeats = seatRepository.saveAll(newSeats);
        return savedSeats.stream().map(seatMapper::toResponse).collect(Collectors.toList());
    }

    // 7️⃣ UPDATE ROW TYPE
    @Transactional
    public List<SeatResponse> updateSeatRowType(BulkSeatUpdateRequest request) {
        SeatType newSeatType = seatTypeRepository.findById(request.getNewTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế mới không tồn tại"));

        List<Seat> seats = seatRepository.findAllByAuditoriumAuditoriumIDAndSeatRow(
                request.getAuditoriumID(), request.getSeatRowToUpdate().toUpperCase());

        if (seats.isEmpty())
            throw new EntityNotFoundException("Không tìm thấy ghế nào trong dãy " + request.getSeatRowToUpdate());

        for (Seat seat : seats)
            seat.setSeatType(newSeatType);

        List<Seat> saved = seatRepository.saveAll(seats);
        return saved.stream().map(seatMapper::toResponse).collect(Collectors.toList());
    }

    // 8️⃣ BULK UPDATE ROW (GỘP / TÁCH)
    @Transactional
    public List<SeatResponse> bulkUpdateSeatRow(BulkSeatUpdateRequest request) {
        // ===== Lấy loại ghế mới (nếu có) =====
        SeatType newSeatType = null;
        if (request.getNewTypeID() != null) {
            newSeatType = seatTypeRepository.findById(request.getNewTypeID())
                    .orElseThrow(() -> new EntityNotFoundException("Loại ghế mới không tồn tại"));
        }

        // ===== Lấy trạng thái mới (nếu có) =====
        Seat.SeatStatus newSeatStatus = null;
        if (request.getNewStatus() != null && !request.getNewStatus().isEmpty()) {
            try {
                newSeatStatus = Seat.SeatStatus.valueOf(request.getNewStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái ghế không hợp lệ: " + request.getNewStatus());
            }
        }

        // ===== Lấy toàn bộ ghế trong dãy =====
        List<Seat> seatsInRow = seatRepository.findAllByAuditoriumAuditoriumIDAndSeatRow(
                request.getAuditoriumID(), request.getSeatRowToUpdate().toUpperCase());

        if (seatsInRow.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy ghế nào trong dãy " + request.getSeatRowToUpdate());
        }

        // ===== 1️⃣ Xử lý GỘP GHẾ (Couple Seat) =====
        if (Boolean.TRUE.equals(request.getIsConvertCoupleSeat())) {
            // ✅ Chỉ cho phép gộp nếu loại ghế mới thực sự là "Couple"
            if (newSeatType == null || !newSeatType.getTypeName().toLowerCase().contains("couple")) {
                throw new IllegalArgumentException("Để gộp ghế, loại ghế mới phải là 'Couple'.");
            }
            return processCoupleSeatConversion(seatsInRow, newSeatType, newSeatStatus);
        }

        // ===== 2️⃣ Xử lý TÁCH GHẾ =====
        if (Boolean.TRUE.equals(request.getIsSeparateCoupleSeat())) {
            SeatType defaultSingleSeatType = newSeatType;
            if (defaultSingleSeatType == null) {
                // fallback mặc định ghế đơn ID = 1
                defaultSingleSeatType = seatTypeRepository.findById(1)
                        .orElseThrow(() -> new EntityNotFoundException("Loại ghế đơn mặc định (ID 1) không tồn tại."));
            }
            return processSingleSeatSeparation(seatsInRow, defaultSingleSeatType, newSeatStatus);
        }

        // ===== 3️⃣ Trường hợp chỉ đổi loại hoặc trạng thái =====
        for (Seat seat : seatsInRow) {
            if (newSeatType != null) {
                seat.setSeatType(newSeatType);
            }
            if (newSeatStatus != null) {
                seat.setStatus(newSeatStatus);
            }
        }

        List<Seat> savedSeats = seatRepository.saveAll(seatsInRow);
        return savedSeats.stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 🔹 GỘP GHẾ ĐÔI
    private List<SeatResponse> processCoupleSeatConversion(List<Seat> seatsInRow, SeatType coupleSeatType, Seat.SeatStatus newSeatStatus) {
        List<Seat> toDelete = new ArrayList<>();
        List<Seat> toUpdate = new ArrayList<>();
        String row = seatsInRow.get(0).getSeatRow();

        for (Seat seat : seatsInRow) {
            if (seat.getColumnNumber() % 2 == 0) {
                toDelete.add(seat);
            } else {
                seat.setSeatType(coupleSeatType);
                if (newSeatStatus != null) seat.setStatus(newSeatStatus);
                seat.setSeatNumber(row + seat.getColumnNumber() + "-" + (seat.getColumnNumber() + 1));
                toUpdate.add(seat);
            }
        }

        seatRepository.deleteAll(toDelete);
        List<Seat> saved = seatRepository.saveAll(toUpdate);
        return saved.stream().map(seatMapper::toResponse).collect(Collectors.toList());
    }

    // 🔹 TÁCH GHẾ ĐÔI
    private List<SeatResponse> processSingleSeatSeparation(List<Seat> seatsInRow, SeatType singleSeatType, Seat.SeatStatus newSeatStatus) {
        List<Seat> toUpdate = new ArrayList<>();
        List<Seat> toCreate = new ArrayList<>();
        String row = seatsInRow.get(0).getSeatRow();
        Auditorium auditorium = seatsInRow.get(0).getAuditorium();

        for (Seat seat : seatsInRow) {
            if (seat.getSeatNumber().contains("-")) {
                seat.setSeatType(singleSeatType);
                if (newSeatStatus != null) seat.setStatus(newSeatStatus);

                int colOdd = seat.getColumnNumber();
                seat.setSeatNumber(row + colOdd);
                toUpdate.add(seat);

                int colEven = colOdd + 1;
                toCreate.add(Seat.builder()
                        .auditorium(auditorium)
                        .seatType(singleSeatType)
                        .seatRow(row)
                        .columnNumber(colEven)
                        .seatNumber(row + colEven)
                        .status(newSeatStatus != null ? newSeatStatus : Seat.SeatStatus.AVAILABLE)
                        .build());
            } else {
                seat.setSeatType(singleSeatType);
                if (newSeatStatus != null) seat.setStatus(newSeatStatus);
                toUpdate.add(seat);
            }
        }

        List<Seat> savedUpdated = seatRepository.saveAll(toUpdate);
        List<Seat> savedCreated = seatRepository.saveAll(toCreate);
        savedUpdated.addAll(savedCreated);
        return savedUpdated.stream().map(seatMapper::toResponse).collect(Collectors.toList());
    }

    // ==================== 🔹 LẤY GHẾ THEO PHÒNG ====================
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByAuditorium(Integer auditoriumId) {
        List<Seat> seats = seatRepository.findAllByAuditorium_AuditoriumID(auditoriumId);
        if (seats.isEmpty())
            throw new EntityNotFoundException("Không tìm thấy ghế nào trong phòng chiếu ID: " + auditoriumId);
        return seats.stream().map(seatMapper::toResponse).collect(Collectors.toList());
    }

}