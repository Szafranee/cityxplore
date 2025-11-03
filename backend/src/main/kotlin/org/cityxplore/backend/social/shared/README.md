# Sharing POIs - Custom POI Support

## Overview

The shared POIs feature now supports two types of POI sharing:

1. **Existing POIs** - Share POIs from the main `points_of_interest` table
2. **Custom POIs** - Share custom, user-defined POIs that exist only in the context of sharing

## API Usage

### Sharing an Existing POI

`POST /api/shared-pois`

```json
{
  "recipientId": "user-uuid",
  "poiId": "existing-poi-uuid",
  "message": "Check out this amazing place!"
}
```

### Sharing a Custom POI

`POST /api/shared-pois`

```json
{
  "recipientId": "user-uuid",
  "customPoi": {
    "name": "My Secret Spot",
    "description": "Amazing view at sunset",
    "category": "viewpoint",
    "latitude": 52.2297,
    "longitude": 21.0122,
    "imageUrls": [
      "https://example.com/image1.jpg",
      "https://example.com/image2.jpg"
    ]
  },
  "message": "You have to see this place!"
}
```

## Validation Rules

### XOR Constraint

- **Exactly one** of `poiId` or `customPoi` must be provided
- Attempting to provide both or neither will result in `400 BAD_REQUEST`

### Custom POI Validation

- `name`: Required, max 200 characters
- `description`: Optional, max 1000 characters
- `category`: Required, max 50 characters
- `latitude`: Required, valid double
- `longitude`: Required, valid double
- `imageUrls`: Optional list of URLs

### Friendship Requirement

- Users can only share POIs (both existing and custom) with **accepted friends**
- Status must be `ACCEPTED`, not `PENDING`, `DECLINED`, or `BLOCKED`

## Response Format

```json
{
  "id": "shared-poi-uuid",
  "sharerId": "sender-uuid",
  "recipientId": "receiver-uuid",
  "poiId": "existing-poi-uuid",
  // OR null if custom POI
  "poiData": {
    // OR null if existing POI
    "name": "My Secret Spot",
    "description": "Amazing view at sunset",
    "category": "viewpoint",
    "latitude": 52.2297,
    "longitude": 21.0122,
    "imageUrls": [
      "https://..."
    ]
  },
  "message": "You have to see this place!",
  "sharedAt": "2025-01-11T10:30:00",
  "viewedAt": null
}
```

## Database Schema

```sql
CREATE TABLE shared_pois
(
    id           UUID PRIMARY KEY,
    sharer_id    UUID      NOT NULL,
    recipient_id UUID      NOT NULL,
    poi_id       UUID,  -- nullable, references points_of_interest
    poi_data     JSONB, -- nullable, custom POI data
    message      VARCHAR(500),
    shared_at    TIMESTAMP NOT NULL,
    viewed_at    TIMESTAMP,
    CONSTRAINT chk_poi_xor CHECK (
        (poi_id IS NOT NULL AND poi_data IS NULL) OR
        (poi_id IS NULL AND poi_data IS NOT NULL)
        )
);
```

## Use Cases

### 1. Tourist recommending an official attraction

```
User A → shares existing POI (Eiffel Tower) → User B
```

### 2. Local sharing a hidden gem

```
User A → creates custom POI (secret beach) → shares with User B
```

### 3. Friend sharing their favorite spot

```
User A → creates custom POI with photos and description → shares with multiple friends
```

## Advantages of Custom POIs

- **Flexibility** - Share places not in the official database
- **Privacy** - Custom POIs are only visible to the recipient
- **Personalisation** - Add personal descriptions and photos
- **Simplicity** - No need to create official POIs for one-time shares

## Limitations

- Custom POIs are **not** added to the main POI database
- Custom POIs **cannot** be discovered by other users
- Custom POIs are **tied to the shared record** - deleting the share deletes the POI data
- No duplicate detection for custom POIs (users can share the same location multiple times)

## Future Enhancements

- "Promote" custom POI to official POI
- Search/filter custom POIs shared by a user
- Statistics on popular custom POI locations
- Merge similar custom POIs into official ones
