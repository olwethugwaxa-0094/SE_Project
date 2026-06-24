SELECT 'CREATE DATABASE connect_dev'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'connect_dev')\gexec