FROM node:18-slim

# non-root user
RUN useradd -m -u 1001 coder

USER coder
WORKDIR /home/coder

CMD ["node"]
