## create(MovieRequest request) Function
| Category | Test Case | Input | Expected |
|-----------|------------|--------|-----------|
| Happy Path | Create valid movie | MovieRequest(title='Inception') | Movie saved with default poster |
| Edge Case | Missing title | MovieRequest(title='') | throws ValidationException |
| Error Scenario | Invalid request | MovieRequest(title=null) | throws ValidationException |
| Integration | Create movie with existing ID | MovieRequest(title='Inception', id=1) | Movie saved with default poster |
| Edge Case | Empty poster URL | MovieRequest(title='Inception', posterUrl='') | Movie saved with default poster |
| Edge Case | Null poster URL | MovieRequest(title='Inception', posterUrl=null) | Movie saved with default poster |

## update(Integer id, MovieRequest request) Function
| Category | Test Case | Input | Expected |
|-----------|------------|--------|-----------|
| Happy Path | Update valid movie | MovieRequest(title='Inception', posterUrl='new-poster.jpg') | Movie updated with new poster |
| Edge Case | Missing title | MovieRequest(title='', posterUrl='new-poster.jpg') | throws ValidationException |
| Error Scenario | Invalid request | MovieRequest(title=null, posterUrl='new-poster.jpg') | throws ValidationException |
| Integration | Update movie with existing ID | MovieRequest(title='Inception', posterUrl='new-poster.jpg', id=1) | Movie updated with new poster |
| Edge Case | Empty poster URL | MovieRequest(title='Inception', posterUrl='') | Movie updated with default poster |
| Edge Case | Null poster URL | MovieRequest(title='Inception', posterUrl=null) | Movie updated with default poster |
| Edge Case | Poster URL not changed | MovieRequest(title='Inception', posterUrl='old-poster.jpg', id=1) | Movie updated with old poster |

## delete(Integer id) Function
| Category | Test Case | Input | Expected |
|-----------|------------|--------|-----------|
| Happy Path | Delete valid movie | id=1 | Movie deleted |
| Error Scenario | Invalid ID | id=null | throws RuntimeException |
| Error Scenario | Non-existent ID | id=999 | throws RuntimeException |
| Integration | Delete movie with existing ID | id=1 | Movie deleted |
| Edge Case | Delete movie with status 'Ended' | id=1 | Movie status updated to 'Ended' |

## getById(Integer id) Function
| Category | Test Case | Input | Expected |
|-----------|------------|--------|-----------|
| Happy Path | Get valid movie | id=1 | MovieResponse with default poster |
| Error Scenario | Invalid ID | id=null | throws EntityNotFoundException |
| Error Scenario | Non-existent ID | id=999 | throws EntityNotFoundException |
| Integration | Get movie with existing ID | id=1 | MovieResponse with default poster |
| Edge Case | Get movie with status 'Ended' | id=1 | MovieResponse with default poster |

## getAll(String status) Function
| Category | Test Case | Input | Expected |
|-----------|------------|--------|-----------|
| Happy Path | Get all movies | status=null | List of MovieResponse with default poster |
| Edge Case | Get movies by status | status='Ended' | List of MovieResponse with default poster |
| Error Scenario | Invalid status | status=null | List of MovieResponse with default poster |
| Integration | Get movies with existing status | status='Ended' | List of MovieResponse with default poster |
| Edge Case | Get movies with empty status | status='' | List of MovieResponse with default poster |

## filterMovies(MovieFilterRequest request) Function
| Category | Test Case | Input | Expected |
|-----------|------------|--------|-----------|
| Happy Path | Filter valid movies | MovieFilterRequest(title='Inception') | List of MovieResponse with default poster |
| Edge Case | Filter by empty title | MovieFilterRequest(title='') | List of MovieResponse with default poster |
| Edge Case | Filter by null title | MovieFilterRequest(title=null) | List of MovieResponse with default poster |
| Edge Case | Filter by empty genre | MovieFilterRequest(title='Inception', genre='') | List of MovieResponse with default poster |
| Edge Case | Filter by null genre | MovieFilterRequest(title='Inception', genre=null) | List of MovieResponse with default poster |
| Integration | Filter movies with existing criteria | MovieFilterRequest(title='Inception', genre='Action') | List of MovieResponse with default poster |
| Edge Case | Filter movies with empty criteria | MovieFilterRequest(title='', genre='') | List of MovieResponse with default poster |
| Edge Case | Filter movies with null criteria | MovieFilterRequest(title=null, genre=null) | List of MovieResponse with default poster |