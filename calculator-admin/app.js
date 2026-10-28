
const API_URL = "http://localhost:3000/api/calculations";

const elements = {
  message: document.getElementById("message"),
  total: document.getElementById("totalCount"),
  add: document.getElementById("addCount"),
  subtract: document.getElementById("subtractCount"),
  multiply: document.getElementById("multiplyCount"),
  divide: document.getElementById("divideCount"),
  body: document.getElementById("historyBody"),
  search: document.getElementById("searchInput"),
  recordCount: document.getElementById("recordCount"),
  refresh: document.getElementById("refreshButton")
};

let calculations = [];

function getOperator(item) {
  const value = String(item.operator ?? "").trim();

  const operators = {
    "+": "+",
    "-": "-",
    "*": "*",
    "/": "/",
    "×": "*",
    "÷": "/"
  };

  return operators[value] ?? value;
}

function formatNumber(value) {
  if (value === null || value === undefined) {
    return "—";
  }

  const number = Number(value);

  return Number.isFinite(number)
    ? String(number)
    : String(value);
}

function formatDate(value) {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return date.toLocaleString();
}

function updateStatistics() {
  elements.total.textContent = calculations.length;

  const counts = {
    "+": 0,
    "-": 0,
    "*": 0,
    "/": 0
  };

  calculations.forEach(item => {
    const operator = getOperator(item);

    if (Object.hasOwn(counts, operator)) {
      counts[operator]++;
    }
  });

  elements.add.textContent = counts["+"];
  elements.subtract.textContent = counts["-"];
  elements.multiply.textContent = counts["*"];
  elements.divide.textContent = counts["/"];
}

function makeCell(value) {
  const cell = document.createElement("td");

  // textContent displays data safely instead of interpreting it as HTML.
  cell.textContent = value ?? "—";

  return cell;
}

function renderHistory() {
  const query = elements.search.value.trim().toLowerCase();

  const filtered = calculations.filter(item => {
    const expression =
      `${item.a} ${getOperator(item)} ${item.b} ${item.result}`;

    return (
      String(item.id ?? "").toLowerCase().includes(query) ||
      expression.toLowerCase().includes(query) ||
      String(item.created_at ?? "").toLowerCase().includes(query)
    );
  });

  elements.body.replaceChildren();

  if (filtered.length === 0) {
    const row = document.createElement("tr");
    const cell = makeCell("No matching calculations found.");
    cell.colSpan = 5;
    row.appendChild(cell);
    elements.body.appendChild(row);
  } else {
    filtered.forEach(item => {
      const row = document.createElement("tr");
      const operator = getOperator(item);

      row.appendChild(makeCell(item.id));
      row.appendChild(
        makeCell(`${item.a} ${operator} ${item.b}`)
      );
      row.appendChild(makeCell(formatNumber(item.result)));
      row.appendChild(makeCell(operator));
      row.appendChild(makeCell(formatDate(item.created_at)));

      elements.body.appendChild(row);
    });
  }

  elements.recordCount.textContent =
    `Showing ${filtered.length} of ${calculations.length} records`;
}

async function loadCalculations() {
  elements.refresh.disabled = true;
  elements.message.textContent = "Loading calculations...";

  try {
    const response = await fetch(API_URL);

    if (!response.ok) {
      throw new Error(`Server returned HTTP ${response.status}`);
    }

    const data = await response.json();

    // Support either a direct array or { calculations: [...] }.
    const records = Array.isArray(data)
      ? data
      : data.calculations;

    if (!Array.isArray(records)) {
      throw new Error(
        "Unexpected API response. Expected an array of calculations."
      );
    }

    calculations = records;

    updateStatistics();
    renderHistory();

    elements.message.textContent =
      `Connected successfully. Loaded ${calculations.length} records.`;
  } catch (error) {
    console.error("Could not load calculations:", error);

    elements.message.textContent =
      `Could not load data: ${error.message}. Check that your backend is running and CORS is configured.`;

    elements.body.replaceChildren();

    const row = document.createElement("tr");
    const cell = makeCell("Calculation history is unavailable.");
    cell.colSpan = 5;
    row.appendChild(cell);
    elements.body.appendChild(row);
  } finally {
    elements.refresh.disabled = false;
  }
}

elements.search.addEventListener("input", renderHistory);
elements.refresh.addEventListener("click", loadCalculations);

loadCalculations();
