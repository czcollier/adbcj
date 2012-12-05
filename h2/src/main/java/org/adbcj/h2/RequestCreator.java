package org.adbcj.h2;

import org.adbcj.*;
import org.adbcj.h2.decoding.*;
import org.adbcj.h2.packets.*;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultDbSessionFuture;

/**
 * @author roman.stoffel@gamlor.info
 */
public class RequestCreator {
    private final H2Connection connection;

    public RequestCreator(H2Connection connection) {
        this.connection = connection;
    }


    public Request createCloseRequest() {
        DefaultDbSessionFuture<Void> future = new DefaultDbSessionFuture<Void>(connection);
        return new Request("Close-Request", future, new CloseConnection(future, connection), new CloseCommand());
    }

    public <T> Request createQuery(String sql,
                                          ResultHandler<T> eventHandler,
                                          T accumulator) {
        final int sessionId = connection.nextId();
        final int queryId = connection.nextId();
        DefaultDbSessionFuture<T> resultFuture = new DefaultDbSessionFuture<T>(connection);
        final Request executeQuery = executeQueryAndClose(sql, eventHandler, accumulator, resultFuture, sessionId, queryId);
        return new Request("Prepare Query: " + sql,
                resultFuture,
                StatementPrepare.continueWithRequest(executeQuery, resultFuture),
                new QueryPrepareCommand(sessionId, sql),
                executeQuery);
    }

    public Request executeUpdate(String sql) {
        final int sessionId = connection.nextId();
        DefaultDbSessionFuture<Result> resultFuture = new DefaultDbSessionFuture<Result>(connection);
        final Request executeQuery = executeUpdateAndClose(sql, resultFuture, sessionId);
        return new Request("Prepare Query: " + sql, resultFuture,
                StatementPrepare.continueWithRequest(executeQuery, resultFuture),
                new QueryPrepareCommand(sessionId, sql),
                executeQuery);
    }

    public Request executePrepareQuery(String sql) {
        DefaultDbSessionFuture<PreparedQuery> resultFuture = new DefaultDbSessionFuture<PreparedQuery>(connection);
        final int sessionId = connection.nextId();
        return new Request("Prepare Query: " + sql, resultFuture,
                StatementPrepare.createPrepareQuery(resultFuture, sessionId), new QueryPrepareCommand(sessionId, sql));
    }


    public Request executePrepareUpdate(String sql) {
        DefaultDbSessionFuture<PreparedUpdate> resultFuture = new DefaultDbSessionFuture<PreparedUpdate>(connection);
        final int sessionId = connection.nextId();
        return new Request("Prepare Update: " + sql, resultFuture,
                StatementPrepare.createPrepareUpdate(resultFuture, sessionId), new QueryPrepareCommand(sessionId, sql));
    }

    public <T> Request executeQueryStatement(ResultHandler<T> eventHandler,
                                                    T accumulator,
                                                    int sessionId,
                                                    Object[] params) {
        DefaultDbSessionFuture<T> resultFuture = new DefaultDbSessionFuture<T>(connection);
        int queryId = connection.nextId();
        return new Request("ExecutePreparedQuery: ", resultFuture,
                new QueryHeader<T>(SafeResultHandlerDecorator.wrap(eventHandler, resultFuture),
                        accumulator,
                        resultFuture), new QueryExecute(sessionId, queryId, params));
    }
    public Request executeUpdateStatement(int sessionId,
                                                 Object[] params) {
        DefaultDbSessionFuture<Result> resultFuture = new DefaultDbSessionFuture<Result>(connection);
        return new Request("ExecutePreparedUpdate: ", resultFuture,
                new UpdateResult(resultFuture),
                new CompoundCommand(
                        new UpdateExecute(sessionId,params),
                        new QueryExecute(connection.idForAutoId(), connection.nextId())));
    }
    public Request executeCloseStatement() {
        DefaultDbSessionFuture<Void> resultFuture = new DefaultDbSessionFuture<Void>(connection);
        final int sessionId = connection.nextId();
        return new Request("ExecuteCloseStatement: ", resultFuture,
                new AnswerNextRequest(connection), new CommandClose(sessionId, resultFuture));
    }

    public Request createGetAutoIdStatement(DefaultDbFuture<Connection> completeConnection) {
        final int sessionId = connection.idForAutoId();
        String sql = "SELECT SCOPE_IDENTITY() WHERE SCOPE_IDENTITY() IS NOT NULL";
        return new Request("Prepare Query: " + sql, completeConnection,
                StatementPrepare.createAutoIdCompletion(completeConnection, connection), new QueryPrepareCommand(sessionId, sql));
    }

    public Request beginTransaction(){
        return new Request("Begin Transacton",new DefaultDbSessionFuture(connection),
                new AwaitOk(connection),
                new AutoCommitChangeCommand(AutoCommitChangeCommand.AutoCommit.AUTO_COMMIT_OFF) );

    }
    public Request endTransaction(){
        return new Request("Begin Transacton",new DefaultDbSessionFuture(connection),
                new AwaitOk(connection),
                new AutoCommitChangeCommand(AutoCommitChangeCommand.AutoCommit.AUTO_COMMIT_ON) );

    }

    <T> Request executeQueryAndClose(String sql, ResultHandler<T> eventHandler,
                                            T accumulator,
                                            DefaultDbSessionFuture<T> resultFuture,
                                            int sessionId,
                                            int queryId) {
        return new Request("ExecuteQuery: " + sql, resultFuture,
                new QueryHeader<T>(SafeResultHandlerDecorator.wrap(eventHandler, resultFuture),
                        accumulator,
                        resultFuture), new CompoundCommand(new QueryExecute(sessionId, queryId), new CommandClose(sessionId)));
    }

    <T> Request executeUpdateAndClose(String sql,
                                             DefaultDbSessionFuture<Result> resultFuture,
                                             int sessionId) {
        H2Connection connection = (H2Connection) resultFuture.getSession();
        return new Request("UpdateExecute: " + sql, resultFuture,
                new UpdateResult(resultFuture),
                new CompoundCommand(
                        new UpdateExecute(sessionId),
                        new QueryExecute(connection.idForAutoId(), connection.nextId()),
                        new CommandClose(sessionId)));
    }
}
