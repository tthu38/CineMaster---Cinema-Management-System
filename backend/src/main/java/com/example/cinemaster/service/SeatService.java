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
                String seatNumber = seatRow + c;
                newSeats.add(Seat.builder()
                        .auditorium(auditorium)
                        .seatType(seatType)
                        .seatRow(seatRow)
                        .columnNumber(c)
                        .seatNumber(seatNumber)
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
        SeatType newSeatType = null;
        if (request.getNewTypeID() != null)
            newSeatType = seatTypeRepository.findById(request.getNewTypeID())
                    .orElseThrow(() -> new EntityNotFoundException("Loại ghế mới không tồn tại"));

        Seat.SeatStatus newSeatStatus = null;
        if (request.getNewStatus() != null && !request.getNewStatus().isEmpty())
            newSeatStatus = Seat.SeatStatus.valueOf(request.getNewStatus().toUpperCase());

        List<Seat> seatsInRow = seatRepository.findAllByAuditoriumAuditoriumIDAndSeatRow(
                request.getAuditoriumID(), request.getSeatRowToUpdate().toUpperCase());

        if (seatsInRow.isEmpty())
            throw new EntityNotFoundException("Không tìm thấy ghế nào trong dãy " + request.getSeatRowToUpdate());

        if (Boolean.TRUE.equals(request.getIsConvertCoupleSeat())) {
            if (newSeatType == null)
                throw new IllegalArgumentException("Phải chỉ định ID loại ghế đôi để thực hiện chuyển đổi GỘP.");
            return processCoupleSeatConversion(seatsInRow, newSeatType, newSeatStatus);
        }

        if (Boolean.TRUE.equals(request.getIsSeparateCoupleSeat())) {
            SeatType defaultSingleSeatType = newSeatType;
            if (defaultSingleSeatType == null)
                defaultSingleSeatType = seatTypeRepository.findById(1)
                        .orElseThrow(() -> new EntityNotFoundException("Loại ghế đơn mặc định (ID 1) không tồn tại."));
            return processSingleSeatSeparation(seatsInRow, defaultSingleSeatType, newSeatStatus);
        }

        for (Seat seat : seatsInRow) {
            if (newSeatType != null) seat.setSeatType(newSeatType);
            if (newSeatStatus != null) seat.setStatus(newSeatStatus);
        }

        List<Seat> savedSeats = seatRepository.saveAll(seatsInRow);
        return savedSeats.stream().map(seatMapper::toResponse).collect(Collectors.toList());
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
}
