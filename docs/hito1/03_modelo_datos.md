# Hito 1 — Modelo de datos (E/R)

## Entidades principales
### User
- userId (PK)
- email
- displayName
- photoUrl (opcional)

### City
- cityId (PK)
- name
- country

### Place
- placeId (PK)
- cityId (FK -> City)
- name
- category
- priceEstimated

### Trip
- tripId (PK)
- cityId (FK -> City)
- days
- totalBudget
- estimatedLodging
- estimatedFood
- estimatedActivities
- createdAt

### TripPlace (N:M)
- tripId (FK -> Trip)
- placeId (FK -> Place)

### Post
- postId (PK)
- authorUserId (FK -> User)
- title
- createdAt

### Comment
- commentId (PK)
- postId (FK -> Post)
- authorUserId (FK -> User)
- text
- createdAt