document.addEventListener("DOMContentLoaded", () => {
    const castWrapper = document.querySelector(".cast-wrapper");
    const castCarousel = document.querySelector(".cast-carousel");
    const castCards = document.querySelectorAll(".cast-card");
    const prevCastBtn = document.querySelector(".prev-cast");
    const nextCastBtn = document.querySelector(".next-cast");

    // mặc định nhân vật thứ 2 (index = 2) ra giữa khi load
    let currentIndex = 2;

    function updateCast() {
        // reset trạng thái
        castCards.forEach((card, i) => {
            card.classList.remove("center", "near", "fade");

            if (i === currentIndex) {
                card.classList.add("center"); // nhân vật chính giữa
            } else if (i === currentIndex - 1 || i === currentIndex + 1) {
                card.classList.add("near");   // ảnh liền kề
            } else if (i === currentIndex - 2 || i === currentIndex + 2) {
                card.classList.add("fade");   // ảnh xa
            } else {
                card.classList.add("fade");
            }
        });

        // tính offset để nhân vật luôn ra giữa
        const wrapperWidth = castWrapper.offsetWidth;
        const cardWidth = castCards[0].offsetWidth + 20; // card + gap
        const centerOffset = (wrapperWidth / 2) - (cardWidth / 2);

        const offset = currentIndex * cardWidth - centerOffset;
        castCarousel.style.transform = `translateX(-${offset}px)`;
    }

    // nút next
    nextCastBtn.addEventListener("click", () => {
        currentIndex = (currentIndex + 1) % castCards.length; // loop
        updateCast();
    });

    // nút prev
    prevCastBtn.addEventListener("click", () => {
        currentIndex = (currentIndex - 1 + castCards.length) % castCards.length; // loop
        updateCast();
    });

    // responsive
    window.addEventListener("resize", updateCast);

    // chạy lần đầu để nhân vật mặc định ra giữa
    updateCast();
});
