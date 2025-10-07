package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.BulkSeatRequest;
import com.example.cinemaster.dto.request.BulkSeatUpdateRequest;
import com.example.cinemaster.dto.request.SeatRequest;
import com.example.cinemaster.dto.response.SeatResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.entity.Seat;
import com.example.cinemaster.entity.SeatType;
import com.example.cinemaster.repository.AuditoriumRepository;
import com.example.cinemaster.repository.SeatRepository;
import com.example.cinemaster.repository.SeatTypeRepository; // Giả định Repository này tồn tại
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SeatService {

    private final SeatRepository seatRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatTypeRepository seatTypeRepository;

    public SeatService(SeatRepository seatRepository, AuditoriumRepository auditoriumRepository, SeatTypeRepository seatTypeRepository) {
        this.seatRepository = seatRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.seatTypeRepository = seatTypeRepository;
    }

    // --- HÀM MAPPER (Ánh xạ từ Entity sang Response DTO) ---
    private SeatResponse mapToResponse(Seat seat) {
        // Lấy thông tin Auditorium
        Integer audId = seat.getAuditorium() != null ? seat.getAuditorium().getAuditoriumID() : null;
        String audName = seat.getAuditorium() != null ? seat.getAuditorium().getName() : "N/A";

        // Lấy thông tin SeatType
        Integer typeId = seat.getSeatType() != null ? seat.getSeatType().getTypeID() : null;
        String typeName = seat.getSeatType() != null ? seat.getSeatType().getTypeName() : "N/A";

        return SeatResponse.builder()
                .seatID(seat.getSeatID())
                .seatNumber(seat.getSeatNumber())
                .seatRow(seat.getSeatRow())
                .columnNumber(seat.getColumnNumber())
                .status(seat.getStatus() != null ? seat.getStatus().name() : null)
                .auditoriumID(audId)
                .auditoriumName(audName)
                .typeID(typeId)
                .typeName(typeName)
                .build();
    }

    // 1. READ ALL
    public List<SeatResponse> getAllSeats() {
        return seatRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 2. READ ONE
    public SeatResponse getSeatById(Integer id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ghế không tìm thấy với ID: " + id));
        return mapToResponse(seat);
    }

    // 3. CREATE
    public SeatResponse createSeat(SeatRequest request) {
        // Kiểm tra và lấy Auditorium Entity
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tìm thấy với ID: " + request.getAuditoriumID()));

        // Kiểm tra và lấy SeatType Entity
        SeatType seatType = seatTypeRepository.findById(request.getTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế không tìm thấy với ID: " + request.getTypeID()));
        Seat.SeatStatus seatStatus = Seat.SeatStatus.valueOf(request.getStatus().toUpperCase());
        Seat seat = Seat.builder()
                .auditorium(auditorium)
                .seatType(seatType)
                .seatNumber(request.getSeatNumber())
                .seatRow(request.getSeatRow())
                .columnNumber(request.getColumnNumber())
                .status(seatStatus)
                .build();

        Seat created = seatRepository.save(seat);
        return mapToResponse(created);
    }

    // 4. UPDATE
    public SeatResponse updateSeat(Integer id, SeatRequest request) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ghế không tìm thấy với ID: " + id));

        // Kiểm tra và cập nhật Auditorium
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tìm thấy với ID: " + request.getAuditoriumID()));

        // Kiểm tra và cập nhật SeatType
        SeatType seatType = seatTypeRepository.findById(request.getTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế không tìm thấy với ID: " + request.getTypeID()));

        // Cập nhật các trường
        seat.setStatus(Seat.SeatStatus.valueOf(request.getStatus().toUpperCase()));
        seat.setAuditorium(auditorium);
        seat.setSeatType(seatType);
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatRow(request.getSeatRow());
        seat.setColumnNumber(request.getColumnNumber());


        Seat updated = seatRepository.save(seat);
        return mapToResponse(updated);
    }

    // 5. DELETE
    public void deleteSeat(Integer id) {
        if (!seatRepository.existsById(id)) {
            throw new EntityNotFoundException("Ghế không tìm thấy với ID: " + id);
        }
        seatRepository.deleteById(id);
    }
    @Transactional
    public List<SeatResponse> createBulkSeats(BulkSeatRequest request) {

        // 1. Kiểm tra tồn tại các khóa ngoại (Auditorium, SeatType)
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tồn tại"));

        SeatType seatType = seatTypeRepository.findById(request.getTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế không tồn tại"));

        List<Seat> newSeats = new ArrayList<>();
        char currentRowChar = request.getStartRowChar().charAt(0);

        // 2. Lặp qua các dãy (Row)
        for (int r = 0; r < request.getRowCount(); r++) {
            String seatRow = String.valueOf((char)(currentRowChar + r)); // A, B, C...

            // 3. Lặp qua các cột (Column) trong mỗi dãy
            for (int c = 1; c <= request.getColumnCount(); c++) {

                String seatNumber = seatRow + c; // Ví dụ: A1, A2, B1, B2...

                Seat seat = Seat.builder()
                        .auditorium(auditorium) // Gán Khóa ngoại
                        .seatType(seatType)      // Gán Khóa ngoại
                        .seatRow(seatRow)
                        .columnNumber(c)
                        .seatNumber(seatNumber)
                        .status(Seat.SeatStatus.AVAILABLE) // Mặc định là Available
                        .build();

                newSeats.add(seat);
            }
        }

        // 4. Lưu tất cả vào database trong một lần giao dịch
        List<Seat> savedSeats = seatRepository.saveAll(newSeats);

        // 5. Trả về danh sách Response
        return savedSeats.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    // HÀM CẬP NHẬT DÃY GHẾ HÀNG LOẠT
    @Transactional
    public List<SeatResponse> updateSeatRowType(BulkSeatUpdateRequest request) {

        // 1. Kiểm tra tồn tại các khóa ngoại
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tồn tại"));

        SeatType newSeatType = seatTypeRepository.findById(request.getNewTypeID())
                .orElseThrow(() -> new EntityNotFoundException("Loại ghế mới không tồn tại"));

        String rowToUpdate = request.getSeatRowToUpdate().toUpperCase();

        // 2. Tìm tất cả ghế trong phòng chiếu và dãy ghế cụ thể
        // Giả định bạn đã có hàm FindAllByAuditoriumIdAndSeatRow trong SeatRepository
        List<Seat> seatsToUpdate = seatRepository.findAllByAuditoriumAuditoriumIDAndSeatRow(
                request.getAuditoriumID(),
                rowToUpdate
        );

        if (seatsToUpdate.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy ghế nào trong dãy " + rowToUpdate + " của phòng chiếu này.");
        }

        // 3. Thực hiện cập nhật
        for (Seat seat : seatsToUpdate) {
            seat.setSeatType(newSeatType); // Sửa loại ghế thành VIP
            // seat.setStatus(Seat.SeatStatus.Broken); // Nếu bạn muốn cập nhật status
        }

        // 4. Lưu tất cả thay đổi (Hibernate/JPA sẽ tự động phát hiện thay đổi và cập nhật)
        List<Seat> savedSeats = seatRepository.saveAll(seatsToUpdate);

        // 5. Trả về danh sách Response đã cập nhật
        return savedSeats.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    @Transactional
    public List<SeatResponse> bulkUpdateSeatRow(BulkSeatUpdateRequest request) {

        // 1. Kiểm tra khóa ngoại
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumID())
                .orElseThrow(() -> new EntityNotFoundException("Phòng chiếu không tồn tại"));

        SeatType newSeatType = null;
        if (request.getNewTypeID() != null) {
            newSeatType = seatTypeRepository.findById(request.getNewTypeID())
                    .orElseThrow(() -> new EntityNotFoundException("Loại ghế mới không tồn tại"));
        }

        Seat.SeatStatus newSeatStatus = null;
        if (request.getNewStatus() != null && !request.getNewStatus().isEmpty()) {
            newSeatStatus = Seat.SeatStatus.valueOf(request.getNewStatus().toUpperCase());
        }

        String rowToUpdate = request.getSeatRowToUpdate().toUpperCase();

        // 2. TÌM TẤT CẢ GHẾ trong dãy
        List<Seat> seatsInRow = seatRepository.findAllByAuditoriumAuditoriumIDAndSeatRow(
                request.getAuditoriumID(),
                rowToUpdate
        );

        if (seatsInRow.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy ghế nào trong dãy " + rowToUpdate);
        }

        // 3. XỬ LÝ LOGIC GHẾ ĐÔI/GHẾ ĐƠN (GỘP HOẶC TÁCH)
        if (Boolean.TRUE.equals(request.getIsConvertCoupleSeat())) {
            // Logic GỘP (Đơn -> Đôi): Yêu cầu bắt buộc phải có newTypeID là loại ghế đôi
            if (newSeatType == null) {
                throw new IllegalArgumentException("Phải chỉ định ID loại ghế đôi để thực hiện chuyển đổi GỘP.");
            }
            return processCoupleSeatConversion(seatsInRow, newSeatType, newSeatStatus);
        }

        if (Boolean.TRUE.equals(request.getIsSeparateCoupleSeat())) {
            // Logic TÁCH (Đôi -> Đơn): Không cần newTypeID nếu muốn về loại đơn mặc định (ID = 1)
            // Nếu newTypeID null, ta mặc định nó về loại đơn cơ bản (ID = 1), bạn cần kiểm tra ID này
            SeatType defaultSingleSeatType = newSeatType;
            if (defaultSingleSeatType == null) {
                defaultSingleSeatType = seatTypeRepository.findById(1) // GIẢ ĐỊNH ID 1 LÀ LOẠI GHẾ ĐƠN CƠ BẢN
                        .orElseThrow(() -> new EntityNotFoundException("Loại ghế đơn mặc định (ID 1) không tồn tại."));
            }
            return processSingleSeatSeparation(seatsInRow, defaultSingleSeatType, newSeatStatus);
        }

        // 4. XỬ LÝ LOGIC CẬP NHẬT TRẠNG THÁI/LOẠI GHẾ THÔNG THƯỜNG (Giữ nguyên)
        List<Seat> seatsToUpdate = new ArrayList<>();
        for (Seat seat : seatsInRow) {
            if (newSeatType != null) {
                seat.setSeatType(newSeatType);
            }
            if (newSeatStatus != null) {
                seat.setStatus(newSeatStatus);
            }
            seatsToUpdate.add(seat);
        }

        List<Seat> savedSeats = seatRepository.saveAll(seatsToUpdate);
        return savedSeats.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // HÀM HỖ TRỢ XỬ LÝ LOGIC GỘP GHẾ ĐÔI (Giữ nguyên)
    private List<SeatResponse> processCoupleSeatConversion(List<Seat> seatsInRow, SeatType coupleSeatType, Seat.SeatStatus newSeatStatus) {
        // ... (Logic GỘP GHẾ CŨ CỦA BẠN - Giữ nguyên) ...
        List<Seat> seatsToDelete = new ArrayList<>();
        List<Seat> seatsToUpdate = new ArrayList<>();
        String rowToUpdate = seatsInRow.get(0).getSeatRow();

        for (Seat seat : seatsInRow) {
            if (seat.getColumnNumber() % 2 == 0) {
                // Ghế chẵn (E2, E4...): Đánh dấu để xóa
                seatsToDelete.add(seat);
            } else {
                // Ghế lẻ (E1, E3...): Cập nhật loại ghế và tên
                seat.setSeatType(coupleSeatType);

                // Cập nhật trạng thái nếu có
                if (newSeatStatus != null) {
                    seat.setStatus(newSeatStatus);
                }

                // Đổi tên Ghế: E1 -> E1-2
                String newSeatNumber = rowToUpdate + seat.getColumnNumber() + "-" + (seat.getColumnNumber() + 1);
                seat.setSeatNumber(newSeatNumber);
                seatsToUpdate.add(seat);
            }
        }

        seatRepository.deleteAll(seatsToDelete);
        List<Seat> savedSeats = seatRepository.saveAll(seatsToUpdate);

        return savedSeats.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // --- HÀM HỖ TRỢ XỬ LÝ LOGIC TÁCH GHẾ ĐƠN (MỚI) ---
    private List<SeatResponse> processSingleSeatSeparation(List<Seat> seatsInRow, SeatType singleSeatType, Seat.SeatStatus newSeatStatus) {

        List<Seat> seatsToUpdate = new ArrayList<>();
        List<Seat> seatsToCreate = new ArrayList<>();
        String rowToUpdate = seatsInRow.get(0).getSeatRow();
        Auditorium auditorium = seatsInRow.get(0).getAuditorium();

        // Duyệt qua các ghế hiện tại (chỉ là ghế lẻ/ghế đôi đã gộp)
        for (Seat seat : seatsInRow) {
            // Chỉ xử lý các ghế có tên dạng gộp (ví dụ: A1-2)
            if (seat.getSeatNumber().contains("-")) {

                // 1. Tách Ghế cũ thành Ghế Lẻ (ví dụ: A1-2 -> A1)
                seat.setSeatType(singleSeatType);
                if (newSeatStatus != null) {
                    seat.setStatus(newSeatStatus);
                }

                // Giả định columnNumber của ghế cũ là lẻ (1, 3, 5...)
                Integer colOdd = seat.getColumnNumber();
                seat.setSeatNumber(rowToUpdate + colOdd);
                // Giữ nguyên ColumnNumber, SeatID
                seatsToUpdate.add(seat);

                // 2. Tạo Ghế mới là Ghế Chẵn (ví dụ: A2)
                Integer colEven = colOdd + 1;
                String seatNumberEven = rowToUpdate + colEven;

                // Kiểm tra xem ghế chẵn này đã tồn tại trong danh sách ban đầu chưa (phòng lỗi logic)
                // Trong trường hợp này, vì ta đã xóa nó lúc gộp, nên ta cần tạo mới:
                Seat newEvenSeat = Seat.builder()
                        .auditorium(auditorium)
                        .seatType(singleSeatType)
                        .seatRow(rowToUpdate)
                        .columnNumber(colEven)
                        .seatNumber(seatNumberEven)
                        .status(newSeatStatus != null ? newSeatStatus : Seat.SeatStatus.AVAILABLE) // Hoặc status mặc định
                        .build();

                seatsToCreate.add(newEvenSeat);
            } else {
                // Nếu ghế không phải ghế đôi đã gộp (ví dụ: ghế đơn được update loại ghế)
                seat.setSeatType(singleSeatType);
                if (newSeatStatus != null) {
                    seat.setStatus(newSeatStatus);
                }
                seatsToUpdate.add(seat);
            }
        }

        // 3. Lưu tất cả thay đổi và ghế mới
        List<Seat> savedUpdatedSeats = seatRepository.saveAll(seatsToUpdate);
        List<Seat> savedCreatedSeats = seatRepository.saveAll(seatsToCreate);

        List<Seat> allSavedSeats = new ArrayList<>(savedUpdatedSeats);
        allSavedSeats.addAll(savedCreatedSeats);

        return allSavedSeats.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
}
