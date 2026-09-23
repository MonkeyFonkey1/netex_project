# PostgreSQL și DBeaver

## Stadiu

Pasul 2 este în lucru. `compose.yaml` pornește PostgreSQL 17.11 și păstrează datele într-un volum Docker. Backendul nu este încă legat la bază, iar tabelele aplicației nu sunt create. Primul punct de verificare pentru învățare este conectarea din DBeaver la baza goală.

## Ce reprezintă fiecare componentă

- PostgreSQL este programul care păstrează și gestionează bazele de date.
- Imaginea Docker conține PostgreSQL și fișierele necesare rulării lui.
- Containerul este instanța pornită din acea imagine.
- Volumul `postgres_data` păstrează datele independent de container.
- DBeaver este aplicația prin care vezi bazele, tabelele și rândurile.
- Flyway va executa fișierele SQL care definesc tabelele, la pornirea backendului.

## Pornirea bazei

1. Deschide Docker Desktop și așteaptă pornirea motorului Docker.
2. În rădăcina repository-ului, copiază `.env.example` în `.env` doar dacă nu există deja `.env`. Alege o parolă locală în fișierul nou.
3. Din același director rulează `docker compose up -d --wait postgres`.

`-d` lasă containerul în fundal. `--wait` așteaptă verificarea de disponibilitate. Comanda `pg_isready` din `healthcheck` verifică dacă PostgreSQL acceptă conexiuni; nu verifică parola clientului sau existența tabelelor aplicației. Conectarea din DBeaver verifică și datele de autentificare.

În `compose.yaml`, `image` alege versiunea, `environment` trimite setările bazei către container, `ports` permite accesul de pe laptop, iar `volumes` păstrează datele. `${POSTGRES_DB:-netex}` înseamnă valoarea din mediu sau, dacă lipsește, `netex`. Pentru parolă, expresia `:?` oprește pornirea cu un mesaj clar dacă valoarea lipsește. `$$` din verificarea de disponibilitate lasă variabila să fie citită în container.

Portul este publicat numai pe `127.0.0.1:5432`, pentru acces local. `netex` este numele bazei și, implicit, numele contului PostgreSQL de dezvoltare. Acest cont este creat de imagine cu drepturi de administrare locală și este diferit de viitorii utilizatori ai aplicației.

## Conectarea vizuală în DBeaver

1. Deschide **Database → New Database Connection**.
2. Alege **PostgreSQL**, apoi **Next**.
3. Completează:

| Câmp | Valoare implicită |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `netex` |
| Username | `netex` |
| Password | Valoarea de după `POSTGRES_PASSWORD=` din fișierul `.env` de la rădăcină |

4. Apasă **Test Connection**. La prima folosire, DBeaver poate cere descărcarea driverului PostgreSQL; acesta îi permite să comunice cu serverul.
5. După mesajul de succes, apasă **Finish**.
6. În navigator, extinde conexiunea, apoi **Schemas → public → Tables**. În funcție de configurarea navigatorului, poate apărea și nivelul **Databases → netex**.

La acest punct nu trebuie să vezi `users` sau `contacts`: baza este creată, dar schema aplicației va veni prin Flyway. După aplicarea migrării vei da **Refresh** și vei vedea tabelele. Pentru rândurile unui tabel, folosește **View Data → All Rows**.

O eroare de conexiune refuzată indică de obicei un container oprit sau un port greșit. O eroare de autentificare cere verificarea utilizatorului și parolei. Schimbarea parolei în `.env` după inițializarea volumului nu schimbă automat parola din PostgreSQL.

## Date persistente și oprire

`docker compose stop postgres` oprește containerul. `docker compose up -d --wait postgres` îl pornește din nou. `docker compose down` elimină containerul și rețeaua proiectului, dar păstrează volumul cu date. Opțiunea `down -v` șterge și volumul cu baza; nu o folosi pentru o oprire obișnuită. Volumul local nu este trimis pe GitHub.

## Următoarea parte a pasului 2

Vom conecta backendul la baza de date și vom adăuga `backend/src/main/resources/db/migration/V1__create_users_and_contacts.sql`. Tabelele planificate sunt `users` și `contacts`, legate prin autorul contactului. Definiția exactă și diagrama vor fi adăugate odată cu migrarea.

Backendul rulat din IntelliJ va folosi `localhost:5432`. Mai târziu, din Docker, va folosi numele serviciului `postgres:5432`. Docker Compose citește fișierul `.env` pentru configurația sa; backendul pornit separat din IntelliJ nu primește automat aceste variabile. Configurarea backendului va fi documentată la integrarea Flyway.
