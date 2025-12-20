document.body.addEventListener('htmx:configRequest', (event) => {
  const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

  if (csrfToken && csrfHeader) {
    event.detail.headers[csrfHeader] = csrfToken;
  }
});