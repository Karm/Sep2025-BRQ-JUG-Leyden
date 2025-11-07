section .data
    msg db 'Hello world!', 0xA  ; 0xA is newline
    len equ $ - msg             ; length ^

section .text
    global _start

_start:
    ; Which syscall: write(1, msg, len)
    mov rax, 1        ; syscall number for "write"
    mov rdi, 1        ; fd 1 (stdout)
    mov rsi, msg      ; ptr to msg
    mov rdx, len      ; msg len
    syscall           ; invoke...

    ; Which syscall: exit(0)
    mov rax, 60       ; syscall number for "exit"
    mov rdi, 0        ; exit status 0
    syscall           ; invoke...

