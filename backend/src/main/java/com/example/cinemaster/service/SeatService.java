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

    public List<SeatResponse> getAllSeats() {
        return seatRepository.findAll()
                .stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }

    public SeatResponse getSeatById(Integer id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ghế không tìm thấy với ID: " + id));
        return seatMapper.toResponse(seat);
    }

    public SeatResponse createSeat(SeatRequest request) {
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tìm thấy với ID: " + request.getAuditoriumID()));
        SeatType seatType = seatTypeRepository.findById(request.getTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế không tìm thấy với ID: " + request.getTypeID()));

        Seat seat = seatMapper.toEntity(request, auditorium, seatType);
        Seat saved = seatRepository.save(seat);
        return seatMapper.toResponse(saved);
    }

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
    public void deleteSeat(Integer id) {
        if (!seatRepository.existsById(id))
            throw new EntityNotFoundException("Ghế không tìm thấy với ID: " + id);
        seatRepository.deleteById(id);
    }

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
                        .seatNumber(String.valueOf(c))
                        .status(Seat.SeatStatus.AVAILABLE)
                        .build());
            }
        }
        List<Seat> savedSeats = seatRepository.saveAll(newSeats);
        return savedSeats.stream().map(seatMapper::toResponse).collect(Collectors.toList());
    }

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

    @Transactional
    public List<SeatResponse> bulkUpdateSeatRow(BulkSeatUpdateRequest request) {
        // ===== Lấy loại ghế mới (nếu có) =====
        SeatType newSeatType = null;
        if (request.getNewTypeID() != null) {
            newSeatType = seatTypeRepository.findById(request.getNewTypeID())
                    .orElseThrow(() -> new EntityNotFoundException("Loại ghế mới không tồn tại"));
        }

        Seat.SeatStatus newSeatStatus = null;
        if (request.getNewStatus() != null && !request.getNewStatus().isEmpty()) {
            try {
                newSeatStatus = Seat.SeatStatus.valueOf(request.getNewStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái ghế không hợp lệ: " + request.getNewStatus());
            }
        }

        List<Seat> seatsInRow = seatRepository.findAllByAuditoriumAuditoriumIDAndSeatRow(
                request.getAuditoriumID(), request.getSeatRowToUpdate().toUpperCase());

        if (seatsInRow.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy ghế nào trong dãy " + request.getSeatRowToUpdate());
        }

        if (Boolean.TRUE.equals(request.getIsConvertCoupleSeat())) {
            if (newSeatType == null || !newSeatType.getTypeName().toLowerCase().contains("couple")) {
                throw new IllegalArgumentException("Để gộp ghế, loại ghế mới phải là 'Couple'.");
            }
            return processCoupleSeatConversion(seatsInRow, newSeatType, newSeatStatus);
        }

        if (Boolean.TRUE.equals(request.getIsSeparateCoupleSeat())) {
            SeatType defaultSingleSeatType = newSeatType;
            if (defaultSingleSeatType == null) {
                // fallback mặc định ghế đơn ID = 1
                defaultSingleSeatType = seatTypeRepository.findById(1)
                        .orElseThrow(() -> new EntityNotFoundException("Loại ghế đơn mặc định (ID 1) không tồn tại."));
            }
            return processSingleSeatSeparation(seatsInRow, defaultSingleSeatType, newSeatStatus);
        }

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

    private List<SeatResponse> processCoupleSeatConversion(List<Seat> seatsInRow,
                                                           SeatType coupleSeatType,
                                                           Seat.SeatStatus newSeatStatus) {

        List<Seat> toDelete = new ArrayList<>();
        List<Seat> toUpdate = new ArrayList<>();

        // sort theo seatNumber thực tế
        seatsInRow.sort( (a,b) -> Integer.compare(
                Integer.parseInt(a.getSeatNumber()),
                Integer.parseInt(b.getSeatNumber())
        ));

        String row = seatsInRow.get(0).getSeatRow();

        for (int i = 0; i < seatsInRow.size() - 1; i += 2) {

            Seat left = seatsInRow.get(i);
            Seat right = seatsInRow.get(i + 1);

            int numLeft = Integer.parseInt(left.getSeatNumber());
            int numRight = Integer.parseInt(right.getSeatNumber());

            // update ghế trái → ghế đôi
            left.setSeatType(coupleSeatType);
            left.setSeatNumber(numLeft + "-" + numRight);

            if (newSeatStatus != null)
                left.setStatus(newSeatStatus);

            toUpdate.add(left);

            // ghế phải = bị xoá
            toDelete.add(right);
        }

        seatRepository.deleteAll(toDelete);
        List<Seat> saved = seatRepository.saveAll(toUpdate);

        return saved.stream().map(seatMapper::toResponse).toList();
    }


    private List<SeatResponse> processSingleSeatSeparation(List<Seat> seatsInRow,
                                                           SeatType singleSeatType,
                                                           Seat.SeatStatus newSeatStatus) {

        List<Seat> toUpdate = new ArrayList<>();
        List<Seat> toCreate = new ArrayList<>();

        String row = seatsInRow.get(0).getSeatRow();
        Auditorium auditorium = seatsInRow.get(0).getAuditorium();

        for (Seat seat : seatsInRow) {

            // ghế đôi → tách
            if (seat.getSeatNumber().contains("-")) {

                String[] parts = seat.getSeatNumber().split("-");
                int a = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[1]);

                int col = seat.getColumnNumber();

                // GHẾ TRÁI
                seat.setSeatType(singleSeatType);
                seat.setSeatNumber(String.valueOf(a));   // only number!

                if (newSeatStatus != null)
                    seat.setStatus(newSeatStatus);

                toUpdate.add(seat);

                // GHẾ PHẢI
                Seat right = Seat.builder()
                        .auditorium(auditorium)
                        .seatType(singleSeatType)
                        .seatRow(row)
                        .seatNumber(String.valueOf(b))  // only number!
                        .columnNumber(col + 1)
                        .status(newSeatStatus != null ? newSeatStatus : Seat.SeatStatus.AVAILABLE)
                        .build();

                toCreate.add(right);
            }
            else {
                // ghế đơn bình thường
                seat.setSeatType(singleSeatType);
                if (newSeatStatus != null)
                    seat.setStatus(newSeatStatus);
                toUpdate.add(seat);
            }
        }

        List<Seat> updated = seatRepository.saveAll(toUpdate);
        List<Seat> created = seatRepository.saveAll(toCreate);

        updated.addAll(created);

        return updated.stream().map(seatMapper::toResponse).toList();
    }


    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByAuditorium(Integer auditoriumId) {
        List<Seat> seats = seatRepository.findAllByAuditorium_AuditoriumID(auditoriumId);
        if (seats.isEmpty())
            throw new EntityNotFoundException("Không tìm thấy ghế nào trong phòng chiếu ID: " + auditoriumId);
        return seats.stream().map(seatMapper::toResponse).collect(Collectors.toList());
    }

}