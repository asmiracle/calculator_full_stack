require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const pool = new Pool({
  host: 'localhost',
  port: 5432,
  user: 'postgres',
  password: 'postgres',
  database: 'calculator_db',
});

const app = express();
app.use(cors({ origin: ['http://198.18.1.79:5500', 'http://localhost:5500', 'http://127.0.0.1:5500'] }));
app.use(express.json());

// Get all history
app.get('/api/calculations', async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT * FROM calculations ORDER BY id DESC');
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

// Save a new calculation (your Java app calls this)
app.post('/api/calculations', async (req, res) => {
  try {
    const { expression, result, operator } = req.body;
    const { rows } = await pool.query(
      'INSERT INTO calculations (expression, result, operator) VALUES ($1, $2, $3) RETURNING *',
      [expression, result, operator]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

app.listen(3000, '0.0.0.0', () => console.log('API running on port 3000'));