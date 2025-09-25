-- V1__init.sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS chat_sessions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    title TEXT DEFAULT 'New Chat',
    is_favorite BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'sender') THEN
        CREATE TYPE sender AS ENUM ('user','assistant','system','tool');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender TEXT NOT NULL,
    content TEXT NOT NULL,
    context TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id UUID PRIMARY KEY,
    source TEXT,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1024) -- dimension should match EMBEDDING_DIM
);

CREATE INDEX IF NOT EXISTS idx_sessions_user ON chat_sessions(user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_messages_session ON chat_messages(session_id, created_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_ivfflat ON knowledge_chunks USING ivfflat (embedding vector_cosine_ops);
