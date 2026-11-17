package repository;

import util.DatabaseException;

import java.util.List;
import java.util.Optional;

/**
 * Generic Repository interface demonstrating Abstraction and Interfaces in Java.
 */
public interface Repository<T, ID> {
    List<T> findAll() throws DatabaseException;
    Optional<T> findById(ID id) throws DatabaseException;
    T save(T entity) throws DatabaseException;
    void deleteById(ID id) throws DatabaseException;
}
