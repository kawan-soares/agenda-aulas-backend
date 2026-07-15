![CI](https://github.com/kawan-soares/agenda-aulas-backend/actions/workflows/ci.yml/badge.svg)

# Agenda Aulas

Sistema de agendamento de aulas de teclado. Professor cadastra horários disponíveis, aluno reserva.
Full-stack: **Spring Boot (Java) + PostgreSQL** no back, **React** no front, autenticação real com **JWT**.

## Por que esse projeto

Não é um CRUD genérico — resolve um problema real: marcar aula de teclado hoje é mensagem solta
no WhatsApp, sem controle de horário. Aqui, o professor define os horários livres, o aluno vê e
reserva, e ninguém marca duas aulas no mesmo horário por acidente (a API garante isso).

## Arquitetura

```
React (frontend, porta 5173)
      ↓  chamadas HTTP com JWT no header
Spring Boot (backend, porta 8080)
      ↓
H2 (local, arquivo em disco) ou PostgreSQL (produção)
```

## Testes

- **Testes unitários** (JUnit 5 + Mockito): cobrem as regras de negócio de `BookingService` e `AvailabilityService` — prevenção de reserva duplicada, permissões de cancelamento/remoção, validação de horários.
- **CI/CD** (GitHub Actions): a cada push ou pull request pra `main`, os testes rodam automaticamente. Veja o badge no topo deste README ou a aba [Actions](https://github.com/kawan-soares/agenda-aulas-backend/actions).
- **Testes E2E** (Cypress): fluxo completo de autenticação, agendamento e regras de negócio, no repositório [agenda-aulas-e2e-tests](https://github.com/kawan-soares/agenda-aulas-e2e-tests).

## Como rodar localmente

### 1. Backend

Pré-requisito: Java 17 e Maven instalados (`java -version` e `mvn -version` pra confirmar).

```bash
cd agenda-aulas
mvn spring-boot:run
```

Se quiser usar o Maven Wrapper (não precisar ter o Maven instalado globalmente), gere ele uma vez com
`mvn -N wrapper:wrapper` e depois use `./mvnw spring-boot:run`.

Isso sobe a API em `http://localhost:8080`, usando o banco H2 (não precisa instalar nada de banco —
ele cria um arquivo `data/agendaaulas.mv.db` sozinho).

Pra ver o banco visualmente: `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:file:./data/agendaaulas`, usuário `sa`, senha vazia)

### 2. Frontend

Pré-requisito: Node.js instalado.

```bash
cd agenda-aulas-frontend
npm install
npm run dev
```

Abre em `http://localhost:5173`.

### 3. Testar o fluxo completo

1. Acesse `http://localhost:5173/cadastro`
2. Crie uma conta como **Professor**
3. Faça login, cadastre um horário disponível
4. Abra uma aba anônima (ou outro navegador), cadastre uma conta como **Aluno**
5. Veja o horário aparecer disponível, reserve
6. Volte pra aba do professor e recarregue — a reserva aparece lá

## Endpoints principais da API

| Método | Rota                          | Quem acessa      | O que faz                          |
|--------|-------------------------------|------------------|-------------------------------------|
| POST   | `/api/auth/register`          | Público          | Cria conta (professor ou aluno)     |
| POST   | `/api/auth/login`             | Público          | Login, retorna token JWT            |
| GET    | `/api/availability`           | Logado           | Lista horários livres               |
| POST   | `/api/availability`           | Professor        | Cria horário disponível             |
| DELETE | `/api/availability/{id}`      | Professor (dono) | Remove horário (se ainda livre)     |
| POST   | `/api/bookings`                | Aluno            | Reserva um horário                  |
| GET    | `/api/bookings/me`             | Logado           | Minhas reservas (aluno ou professor)|
| DELETE | `/api/bookings/{id}`            | Dono da reserva  | Cancela reserva                     |

## Deploy (quando for pra produção)

**Backend**: Render ou Railway, com um banco PostgreSQL gerenciado.
- Defina as variáveis de ambiente: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`
- Ative o perfil `prod`: `SPRING_PROFILES_ACTIVE=prod`

**Frontend**: Netlify ou Vercel.
- Defina `VITE_API_URL` apontando pra URL do backend em produção

**Importante antes de ir pra produção:**
- Troque o `jwt.secret` do `application.yml` por um valor forte e secreto (nunca deixe o valor de exemplo)
- Restrinja o CORS em `SecurityConfig.java` (hoje está liberado pra qualquer origem, ajuste pro domínio real do frontend)

## Próximos passos possíveis
- Notificação por e-mail/WhatsApp quando uma aula é reservada ou cancelada
- Horários recorrentes (toda terça às 15h, por exemplo) em vez de criar um por um
- Página de perfil pra aluno/professor editar dados