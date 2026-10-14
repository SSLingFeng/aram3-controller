/* Plan document interactions: TOC active state + smooth offset */
(function () {
  const toc = document.getElementById("toc");
  if (!toc) return;

  const links = Array.from(toc.querySelectorAll('a[href^="#"]'));
  const map = new Map();
  links.forEach((a) => {
    const id = a.getAttribute("href").slice(1);
    const el = document.getElementById(id);
    if (el) map.set(el, a);
  });

  const setActive = (activeLink) => {
    links.forEach((l) => l.classList.remove("active"));
    if (activeLink) activeLink.classList.add("active");
  };

  const observer = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((e) => e.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio);
      if (visible[0]) {
        setActive(map.get(visible[0].target));
      }
    },
    {
      rootMargin: "-15% 0px -65% 0px",
      threshold: [0, 0.1, 0.25, 0.5, 1],
    }
  );

  map.forEach((_, el) => observer.observe(el));

  // Fallback: first section active on load
  if (links[0]) setActive(links[0]);

  // Copy DDL button is intentionally omitted — plan is read-only
})();
